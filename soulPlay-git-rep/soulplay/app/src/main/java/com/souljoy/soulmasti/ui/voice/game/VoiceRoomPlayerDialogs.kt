package com.souljoy.soulmasti.ui.voice.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.souljoy.soulmasti.R
import com.souljoy.soulmasti.data.firebase.FirebaseUidMapping
import com.souljoy.soulmasti.ui.common.PrimaryActionButton

@Composable
fun VoiceRoomPlayerActionDialog(
    targetUid: String,
    myUid: String?,
    profile: VoiceUserProfile?,
    onDismiss: () -> Unit,
    onAddFriend: () -> Unit,
    onSendGift: () -> Unit,
    accentBlue: Color,
    accentPink: Color,
) {
    val menuName = profile?.username?.takeIf { it.isNotBlank() }
        ?: FirebaseUidMapping.shortLabel(targetUid)
    val isOwnProfile = myUid != null && targetUid == myUid
    val menuPhotoUrl = profile?.profilePictureUrl?.takeIf { it.isNotBlank() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    if (menuPhotoUrl.isNullOrBlank()) {
                        Image(
                            painter = painterResource(R.drawable.ic_mascot_hero),
                            contentDescription = stringResource(R.string.profile_photo_cd),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        AsyncImage(
                            model = menuPhotoUrl,
                            contentDescription = stringResource(R.string.profile_photo_cd),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(R.drawable.ic_mascot_hero),
                            error = painterResource(R.drawable.ic_mascot_hero),
                        )
                    }
                }
                Column {
                    Text(menuName, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (isOwnProfile) stringResource(R.string.you) else stringResource(R.string.soulplay_user),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (isOwnProfile) {
                        stringResource(R.string.mini_profile_self_desc)
                    } else {
                        stringResource(R.string.mini_profile_other_desc)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
                if (!isOwnProfile) {
                    PrimaryActionButton(
                        text = stringResource(R.string.add_friend),
                        onClick = onAddFriend,
                        containerColor = accentBlue,
                    )
                }
                PrimaryActionButton(
                    text = stringResource(R.string.send_gift),
                    onClick = onSendGift,
                    containerColor = accentPink,
                )
                if (isOwnProfile) {
                    Text(
                        text = stringResource(R.string.add_friend_hidden_self),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
fun VoiceRoomGiftRecipientDialog(
    others: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onPickRecipient: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.send_gift_to)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (others.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_other_players),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B)
                    )
                } else {
                    others.forEach { (uid, label) ->
                        TextButton(
                            onClick = { onPickRecipient(uid) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}
