const functions = require("firebase-functions");
const admin = require("firebase-admin");
const logger = functions.logger;
admin.initializeApp();

/* =================================================
   1️⃣ MATCHMAKING - CREATE ROOM
================================================= */

exports.matchmaking = functions.database.onValueCreated(
    "/matchmaking/waitingPlayers/{uid}",
    async (event) => {

        const waitingRef = admin.database().ref("matchmaking/waitingPlayers");
        const waitingSnap = await waitingRef.once("value");
        const waiting = waitingSnap.val();
        if (!waiting) return;

        const players = Object.keys(waiting);
        if (players.length < 4) return;

        const selected = players.slice(0, 4);

        // ✅ Check all players online & ensure totalWinnings exists
        for (let uid of selected) {
            const userRef = admin.database().ref(`users/${uid}`);
            const userSnap = await userRef.once("value");
            const user = userSnap.val();
            if (!user || user.online !== true) return;
            if (user.totalWinnings === undefined) {
                await userRef.update({ totalWinnings: 0 });
            }
        }

        const roomRef = admin.database().ref("rooms").push();
        const roomId = roomRef.key;

        const rolesList = ["RAJA", "MANTRI", "SIPAHI", "CHOR"];
        shuffleArray(rolesList);

        let roles = {};
        let scores = {};
        let playersData = {};

        selected.forEach((uid, index) => {
            roles[uid] = rolesList[index];
            scores[uid] = 0;
            playersData[uid] = { online: true };
        });

        const now = Date.now();

        await roomRef.set({
            status: "PLAYING",
            createdAt: now,
            players: playersData,
            gameState: {
                phase: "RAJA_DECISION",
                roles: roles,
                selectedSuspect: "",
                scores: scores,
                timerEndAt: now + 60000
            }
        });

        for (let uid of selected) {
            await waitingRef.child(uid).remove();
        }
    }
);


/* =================================================
   2️⃣ TIMER ENGINE (AUTO PHASE + TIMEOUT CONTROL)
================================================= */

exports.timerEngine = functions.scheduler.onSchedule("every 1 minutes", async (event) => {

    const roomsSnap = await admin.database().ref("rooms").once("value");
    const rooms = roomsSnap.val();
    if (!rooms) {
        logger.info("timerEngine: no rooms found");
        return;
    }

    const now = Date.now();
    const roomIds = Object.keys(rooms);
    logger.info("timerEngine: checking rooms", { count: roomIds.length, now });

    for (let roomId of roomIds) {

        const room = rooms[roomId];
        if (!room.gameState || room.status !== "PLAYING") {
            logger.debug("timerEngine: skip room", { roomId, status: room?.status, hasGameState: !!room?.gameState });
            continue;
        }

        const gs = room.gameState;
        if (!gs.timerEndAt) {
            logger.debug("timerEngine: skip room (no timerEndAt)", { roomId, phase: gs.phase });
            continue;
        }

        const timerEndAt = typeof gs.timerEndAt === "string" ? parseInt(gs.timerEndAt, 10) : gs.timerEndAt;
        const expired = now >= timerEndAt;
        logger.info("timerEngine: room check", { roomId, phase: gs.phase, timerEndAt, now, expired });

        if (expired) {

            const roomGameRef = admin.database().ref(`rooms/${roomId}/gameState`);

            switch (gs.phase) {

                case "RAJA_DECISION":
                    await roomGameRef.update({
                        phase: "MANTRI_CONFIRM",
                        timerEndAt: now + 60000
                    });
                    logger.info("timerEngine: advanced RAJA_DECISION -> MANTRI_CONFIRM", { roomId });
                    break;

                case "MANTRI_CONFIRM":
                    await roomGameRef.update({
                        phase: "VOICE_DISCUSSION",
                        timerEndAt: now + 60000
                    });
                    break;

                case "VOICE_DISCUSSION":
                    await roomGameRef.update({
                        phase: "MANTRI_GUESS",
                        timerEndAt: now + 60000 // 60 sec guess time
                    });
                    break;

                case "MANTRI_GUESS":

                    // ❌ If no guess in 5 sec → Chor wins
                    if (!gs.selectedSuspect) {

                        let chorUid = null;
                        for (let uid in gs.roles) {
                            if (gs.roles[uid] === "CHOR") chorUid = uid;
                        }

                        if (chorUid) {
                            gs.scores[chorUid] += 1700;
                        }

                        // 🧮 Update users' total winnings
                        const usersUpdates = {};
                        for (let uid in gs.scores) {
                            const delta = gs.scores[uid];
                            if (!delta) continue;
                            usersUpdates[`${uid}/totalWinnings`] = admin.database.ServerValue.increment(delta);
                        }
                        if (Object.keys(usersUpdates).length > 0) {
                            await admin.database().ref("users").update(usersUpdates);
                        }

                        await admin.database()
                            .ref(`rooms/${roomId}`)
                            .update({
                                status: "ENDED",
                                endReason: "MANTRI_TIMEOUT",
                                message: "Mantri did not guess. Chor wins."
                            });

                        await roomGameRef.update({
                            scores: gs.scores,
                            phase: "GAME_OVER",
                            timerEndAt: null
                        });
                    }

                    break;
            }
        }
    }
});


/* =================================================
   3️⃣ MANTRI GUESS LOGIC (NORMAL RESULT)
================================================= */

exports.checkGuess = functions.database.onValueUpdated(
    "/rooms/{roomId}/gameState/selectedSuspect",
    async (event) => {

        const roomId = event.params.roomId;
        const before = event.data.before.val();
        const suspect = event.data.after.val();

        // If suspect is empty / null, or didn't actually change, do nothing
        if (!suspect || suspect === before) {
            return;
        }

        const roomRef = admin.database().ref(`rooms/${roomId}`);
        const roomSnap = await roomRef.once("value");
        const room = roomSnap.val();
        if (!room || !room.gameState || room.status !== "PLAYING") return;

        const roles = room.gameState.roles;
        const scores = room.gameState.scores;

        let chorUid = null;

        for (let uid in roles) {
            if (roles[uid] === "CHOR") chorUid = uid;
        }

        if (!chorUid) return;

        let resultMessage = "";

        if (suspect === chorUid) {

            for (let uid in roles) {
                if (roles[uid] === "RAJA") scores[uid] += 1000;
                if (roles[uid] === "MANTRI") scores[uid] += 500;
                if (roles[uid] === "SIPAHI") scores[uid] += 200;
            }

            resultMessage = "Mantri guessed correctly! Chor caught.";

        } else {

            scores[chorUid] += 1700;
            resultMessage = "Wrong guess! Chor wins.";
        }

        // 🧮 Update users' total winnings
        const usersUpdates = {};
        for (let uid in scores) {
            const delta = scores[uid];
            if (!delta) continue;
            usersUpdates[`${uid}/totalWinnings`] = admin.database.ServerValue.increment(delta);
        }
        if (Object.keys(usersUpdates).length > 0) {
            await admin.database().ref("users").update(usersUpdates);
        }

        await roomRef.update({
            status: "ENDED",
            endReason: "ROUND_FINISHED",
            message: resultMessage
        });

        await roomRef.child("gameState").update({
            scores: scores,
            phase: "GAME_OVER",
            timerEndAt: null
        });

    }
);


/* =================================================
   4️⃣ INIT USER - ENSURE totalWinnings EXISTS
================================================= */

exports.initUser = functions.database.onValueCreated(
    "/users/{uid}",
    async (event) => {
        const uid = event.params.uid;
        const user = event.data.val();
        if (user && user.totalWinnings === undefined) {
            await admin.database().ref(`users/${uid}`).update({ totalWinnings: 200 });
        }
    }
);


/* =================================================
   5️⃣ END GAME IF PLAYER GOES OFFLINE
================================================= */

exports.playerOffline = functions.database.onValueUpdated(
    "/users/{uid}/online",
    async (event) => {

        const before = event.data.before.val();
        const after = event.data.after.val();
        const uid = event.params.uid;

        if (before === true && after === false) {

            const roomsSnap = await admin.database().ref("rooms").once("value");
            const rooms = roomsSnap.val();
            if (!rooms) return;

            for (let roomId in rooms) {

                const room = rooms[roomId];

                if (room.players && room.players[uid] && room.status === "PLAYING") {

                    await admin.database()
                        .ref(`rooms/${roomId}`)
                        .update({
                            status: "ENDED",
                            endReason: "PLAYER_OFFLINE",
                            message: "One player is offline"
                        });
                }
            }
        }
    }
);

/* =================================================
  7️⃣ GIFT LEADERBOARDS (PROFILE + VOICE ROOM)
================================================= */

exports.aggregateGiftLeaderboards = functions.database.onValueCreated(
    "/giftEvents/{segment}/{scopeId}/{eventId}",
    async (event) => {
        const segment = event.params.segment;
        const scopeId = event.params.scopeId;
        const eventId = event.params.eventId;
        const gift = event.data.val() || {};

        const fromUid = typeof gift.fromUserId === "string" ? gift.fromUserId.trim() : "";
        const toUid = typeof gift.toUserId === "string" ? gift.toUserId.trim() : "";
        if (!fromUid) return;

        const soul = resolveGiftSoul(gift);
        const createdAt = toLong(gift.createdAt) || Date.now();

        // Idempotency guard for function retries.
        const dedupeRef = admin.database().ref(`leaderboardsV1/processedGiftEvents/${eventId}`);
        const dedupeSnap = await dedupeRef.once("value");
        if (dedupeSnap.exists()) return;
        await dedupeRef.set({
            processedAt: admin.database.ServerValue.TIMESTAMP,
            segment: segment,
            scopeId: scopeId,
        });

        const dayKey = utcDayKey(createdAt);
        const weekKey = utcWeekKey(createdAt);

        const updates = {};

        // ---------- Profile leaderboards (credit receiver soul) ----------
        if (toUid && soul > 0) {
            updates[`leaderboardsV1/profile/daily/${dayKey}/${toUid}/soul`] = admin.database.ServerValue.increment(soul);
            updates[`leaderboardsV1/profile/weekly/${weekKey}/${toUid}/soul`] = admin.database.ServerValue.increment(soul);
            updates[`leaderboardsV1/profile/total/${toUid}/soul`] = admin.database.ServerValue.increment(soul);
            updates[`leaderboardsV1/profile/lastGiftAt/${toUid}`] = createdAt;
        }

        // ---------- Voice room leaderboards ----------
        if (segment === "voice") {
            updates[`leaderboardsV1/room/daily/${dayKey}/${scopeId}/soul`] = admin.database.ServerValue.increment(soul);
            updates[`leaderboardsV1/room/weekly/${weekKey}/${scopeId}/soul`] = admin.database.ServerValue.increment(soul);
            updates[`leaderboardsV1/room/total/${scopeId}/soul`] = admin.database.ServerValue.increment(soul);
            updates[`leaderboardsV1/room/lastGiftAt/${scopeId}`] = createdAt;
        }

        await admin.database().ref().update(updates);
    },
);

/* =================================================
  8️⃣ COMPETITION REWARDS (TOP-N BY PERIOD)
================================================= */

exports.distributeDailyTop3Rewards = functions.scheduler.onSchedule("10 0 * * *", async () => {
    const now = Date.now();
    const targetDayKey = utcDayKey(now - 86400000); // yesterday
    const rewardId = `daily-${targetDayKey}`;
    await distributeProfileRewards({
        leaderboardPath: `leaderboardsV1/profile/daily/${targetDayKey}`,
        topN: 3,
        rewardId,
    });
});

exports.distributeWeeklyTop5Rewards = functions.scheduler.onSchedule("15 0 * * 1", async () => {
    const now = Date.now();
    const targetWeekKey = utcWeekKey(now - 7 * 86400000); // previous week
    const rewardId = `weekly-${targetWeekKey}`;
    await distributeProfileRewards({
        leaderboardPath: `leaderboardsV1/profile/weekly/${targetWeekKey}`,
        topN: 5,
        rewardId,
    });
});

exports.distributeMonthlyTop10Rewards = functions.scheduler.onSchedule("20 0 1 * *", async () => {
    const now = new Date();
    const prevMonth = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth() - 1, 1));
    const rewardId = `monthly-total-${prevMonth.getUTCFullYear()}-${String(prevMonth.getUTCMonth() + 1).padStart(2, "0")}`;
    await distributeProfileRewards({
        leaderboardPath: "leaderboardsV1/profile/total",
        topN: 10,
        rewardId,
    });
});


/* =================================================
   6️⃣ SHUFFLE FUNCTION
================================================= */

function shuffleArray(array) {
    for (let i = array.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [array[i], array[j]] = [array[j], array[i]];
    }
}

function toLong(value) {
    if (value === null || value === undefined) return 0;
    if (typeof value === "number") return Math.trunc(value);
    if (typeof value === "string") {
        const n = parseInt(value.trim(), 10);
        return Number.isFinite(n) ? n : 0;
    }
    return 0;
}

function utcDayKey(timestampMs) {
    return Math.floor(timestampMs / 86400000);
}

function utcWeekKey(timestampMs) {
    const d = new Date(timestampMs);
    // ISO week-like key, Monday based.
    const date = new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate()));
    const dayNum = date.getUTCDay() || 7;
    date.setUTCDate(date.getUTCDate() + 4 - dayNum);
    const yearStart = new Date(Date.UTC(date.getUTCFullYear(), 0, 1));
    const weekNo = Math.ceil((((date - yearStart) / 86400000) + 1) / 7);
    return date.getUTCFullYear() * 100 + weekNo;
}

function resolveGiftSoul(gift) {
    // Prefer explicit soul fields if present in payload.
    const candidates = [
        gift.soul,
        gift.senderSoul,
        gift.profileSoul,
        gift.contributionSoul,
        gift.receiverSoul,
    ];
    for (const value of candidates) {
        const parsed = toLong(value);
        if (parsed > 0) return parsed;
    }
    return 0;
}

async function distributeProfileRewards({ leaderboardPath, topN, rewardId }) {
    const guardRef = admin.database().ref(`rewardsV1/distributions/${rewardId}`);
    const guardSnap = await guardRef.once("value");
    if (guardSnap.exists()) return;

    const leaderboardSnap = await admin.database().ref(leaderboardPath).once("value");
    const rows = [];
    leaderboardSnap.forEach((child) => {
        const uid = child.key;
        const soul = toLong(child.child("soul").val());
        if (uid && soul > 0) rows.push({ uid, soul });
    });
    rows.sort((a, b) => b.soul - a.soul);
    const winners = rows.slice(0, topN);
    if (winners.length === 0) {
        await guardRef.set({ appliedAt: admin.database.ServerValue.TIMESTAMP, winners: 0 });
        return;
    }

    const updates = {};
    const periodType = rewardPeriodType(rewardId);
    const rewardTitle = rewardTitleForPeriod(periodType, topN);
    winners.forEach((w, idx) => {
        const coinReward = Math.floor(w.soul * 0.10);
        const soulReward = Math.floor(w.soul * 0.05);
        const rank = idx + 1;
        updates[`users/${w.uid}/totalWinnings`] = admin.database.ServerValue.increment(coinReward);
        updates[`users/${w.uid}/soul`] = admin.database.ServerValue.increment(soulReward);
        updates[`rewardsV1/history/${rewardId}/${w.uid}`] = {
            rank,
            sourceSoul: w.soul,
            coinReward,
            soulReward,
            periodType,
            createdAt: admin.database.ServerValue.TIMESTAMP,
        };
        updates[`users/${w.uid}/rewardInbox/${rewardId}`] = {
            rewardId,
            title: rewardTitle,
            periodType,
            rank,
            sourceSoul: w.soul,
            coinReward,
            soulReward,
            read: false,
            createdAt: admin.database.ServerValue.TIMESTAMP,
        };
        updates[`users/${w.uid}/judgeBotMessages/${rewardId}`] = {
            fromUid: "judge_bot",
            fromName: "Judge Bot",
            type: "reward_notice",
            title: rewardTitle,
            text: `Congrats! You ranked #${rank} and received ${coinReward} coins + ${soulReward} soul.`,
            rewardId,
            periodType,
            rank,
            sourceSoul: w.soul,
            coinReward,
            soulReward,
            read: false,
            createdAt: admin.database.ServerValue.TIMESTAMP,
        };
    });
    updates[`rewardsV1/distributions/${rewardId}`] = {
        appliedAt: admin.database.ServerValue.TIMESTAMP,
        winners: winners.length,
        topN,
        leaderboardPath,
    };
    await admin.database().ref().update(updates);
}

function rewardPeriodType(rewardId) {
    if (typeof rewardId !== "string") return "unknown";
    if (rewardId.startsWith("daily-")) return "daily";
    if (rewardId.startsWith("weekly-")) return "weekly";
    if (rewardId.startsWith("monthly-")) return "monthly";
    return "unknown";
}

function rewardTitleForPeriod(periodType, topN) {
    if (periodType === "daily") return `Daily Top ${topN} Reward`;
    if (periodType === "weekly") return `Weekly Top ${topN} Reward`;
    if (periodType === "monthly") return `Monthly Top ${topN} Reward`;
    return `Top ${topN} Reward`;
}




// Example waitingPlayers structure:
// {
//   "matchmaking": {
//     "waitingPlayers": {
//       "uid1": { "online": true },
//       "uid2": { "online": true },
//       "uid3": { "online": true },
//       "uid4": { "online": true }
//     }
//   }
// }



// Example room structure after matchmaking picks players:
//  {
//   "status": "PLAYING",
//   "players": {
//     "uid1": { "online": true },
//     "uid2": { "online": true },
//     "uid3": { "online": true },
//     "uid4": { "online": true }
//   },
//   "gameState": {
//     "phase": "RAJA_DECISION",
//     "roles": { ... },
//     "timerEndAt": 1700000003000
//   }
// }

// Example users structure with totalWinnings initialized:
// {
//   "users": {
//     "uid1": { "online": true, "totalWinnings": 0 },
//     "uid2": { "online": true, "totalWinnings": 0 },
//     "uid3": { "online": true, "totalWinnings": 0 },
//     "uid4": { "online": true, "totalWinnings": 0 }
//   }
// }
