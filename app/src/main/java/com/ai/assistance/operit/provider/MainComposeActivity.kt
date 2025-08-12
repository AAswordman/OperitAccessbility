package com.ai.assistance.operit.provider

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
//
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai.assistance.operit.provider.R
import kotlinx.coroutines.delay
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign

class MainComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainComposeScreen()
            }
        }
    }
}

@Composable
fun MainComposeScreen() {
    var isServiceConnected by remember { mutableStateOf(false) }
    var context = LocalContext.current

    // 定期检查服务状态
    LaunchedEffect(Unit) {
        while (true) {
            isServiceConnected = UIAccessibilityService.isServiceConnected
            delay(1000)
        }
    }

    // 外层相当于 ConstraintLayout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4F8)) // 对应原背景色
    ) {
        // 中间层相当于 CardView
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .align(Alignment.Center), // 使卡片居中
            shape = RoundedCornerShape(16.dp), // 对应 cardCornerRadius
            elevation = CardDefaults.cardElevation(8.dp) // 对应 cardElevation
        ) {
            // 内层相当于 ConstraintLayout
            Column(
                modifier = Modifier.padding(24.dp), // 对应内层padding
                horizontalAlignment = Alignment.CenterHorizontally // 水平居中
            ) {
                // 图标 (原ImageView)
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(80.dp)
                )

                // 应用名称 (原TextView)
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp)
                )

                // 描述文本 (原TextView)
                Text(
                    text = stringResource(R.string.main_activity_description),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )

                // 状态文本 (原TextView)
                Text(
                    text = if (isServiceConnected) "服务状态: 已连接" else "服务状态: 未连接",
                    //color = if (isServiceConnected) Color.Green else Color.Red,
                    color = if (isServiceConnected) Color(0xFF4CAF50) else Color(0xFFF44336),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 24.dp)
                )

                // 设置按钮 (原Button)
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text("打开无障碍设置")
                }
            }
        }
    }
}
