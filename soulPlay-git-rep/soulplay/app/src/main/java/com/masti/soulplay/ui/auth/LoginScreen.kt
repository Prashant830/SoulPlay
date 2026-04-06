package com.masti.soulplay.ui.auth

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import org.koin.androidx.compose.koinViewModel
import androidx.activity.result.ActivityResult
import com.masti.soulplay.R
import androidx.compose.ui.text.style.TextAlign

@Composable
fun LoginScreen(
    onRegistered: () -> Unit,
    modifier: Modifier = Modifier,
    vm: LoginViewModel = koinViewModel(),
) {
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current

    val webClientId = stringResource(id = R.string.default_web_client_id)
    val canSignInWithGoogle = webClientId.isNotBlank() && webClientId != "CHANGE_ME"

    val googleSignInClient = remember {
        // Requires you to set `default_web_client_id` in `res/values/strings.xml`
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(webClientId)
            .build()
            .let { GoogleSignIn.getClient(context, it) }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        val data = result.data
        if (result.resultCode != Activity.RESULT_OK || data == null) {
            // If user cancels / something goes wrong, surface a UI error.
            vm.signInWithGoogleIdToken(idToken = null, onDone = onRegistered)
            return@rememberLauncherForActivityResult
        }
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            vm.signInWithGoogleIdToken(account.idToken, onDone = onRegistered)
        } catch (e: ApiException) {
            // Trigger error in view model by passing null id token.
            vm.signInWithGoogleIdToken(null, onDone = onRegistered)
        }
    }

    val startGoogleSignIn: () -> Unit = {
        launcher.launch(googleSignInClient.signInIntent)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Center logo + title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 100.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_mascot_hero),
                    contentDescription = null,
                    modifier = Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(22.dp)),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = "SoulMasti",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color(0xFF0B071A)
                )
                Text(
                    text = "Party Game & Voice Chat",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B6B7D),
                    textAlign = TextAlign.Center
                )
            }

            // Bottom: messages + Google button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state is LoginUiState.Error) {
                    Text(
                        text = (state as LoginUiState.Error).message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                if (!canSignInWithGoogle) {
                    Text(
                        text = "Set default_web_client_id (Web OAuth client id) in strings.xml",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                OutlinedButton(
                    onClick = startGoogleSignIn,
                    enabled = canSignInWithGoogle && state !is LoginUiState.Loading,
                    modifier = Modifier
                        .fillMaxWidth(0.80f)
                        .height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE0E0E0))
                    ),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.align(Alignment.CenterStart),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_google_logo),
                                contentDescription = "Google",
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        if (state is LoginUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color(0xFF4285F4),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Sign in with Google",
                                color = Color(0xFF0B071A),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

