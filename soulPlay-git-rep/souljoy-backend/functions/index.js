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
                        timerEndAt: now + 120000
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
   6️⃣ SHUFFLE FUNCTION
================================================= */

function shuffleArray(array) {
    for (let i = array.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [array[i], array[j]] = [array[j], array[i]];
    }
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
