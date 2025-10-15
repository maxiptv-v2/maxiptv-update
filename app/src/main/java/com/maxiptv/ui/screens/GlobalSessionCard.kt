package com.maxiptv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxiptv.data.ActiveSession

@Composable
fun GlobalSessionCard(session: ActiveSession, onForceLogout: () -> Unit) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20))
  ) {
    Column(
      modifier = Modifier.padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.Default.Person,
              contentDescription = null,
              tint = Color(0xFF00FF00),
              modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
              text = session.username,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
          
          Spacer(Modifier.height(4.dp))
          
          Text(
            text = "🖥️ ${session.deviceName}",
            fontSize = 14.sp,
            color = Color(0xFFBBBBBB)
          )
          
          val timeSinceLogin = System.currentTimeMillis() - session.loginTime
          val minutesAgo = (timeSinceLogin / (1000 * 60)).toInt()
          val loginText = when {
            minutesAgo < 1 -> "Agora"
            minutesAgo < 60 -> "$minutesAgo min atrás"
            else -> "${minutesAgo / 60}h atrás"
          }
          
          Text(
            text = "⏰ Login: $loginText",
            fontSize = 12.sp,
            color = Color(0xFF999999)
          )
          
          val timeSinceHeartbeat = System.currentTimeMillis() - session.lastHeartbeat
          val heartbeatSeconds = (timeSinceHeartbeat / 1000).toInt()
          val heartbeatText = if (heartbeatSeconds < 30) "🟢 Online" else "🟡 ${heartbeatSeconds}s atrás"
          
          Text(
            text = "💓 $heartbeatText",
            fontSize = 12.sp,
            color = if (heartbeatSeconds < 30) Color(0xFF00FF00) else Color(0xFFFFAA00)
          )
        }
        
        Button(
          onClick = onForceLogout,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
        ) {
          Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("Deslogar", fontSize = 12.sp)
        }
      }
    }
  }
}



