package com.cartechiq.example

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cartechiq.CartechIQService
import com.cartechiq.GroupLookupRequest
import com.cartechiq.Location
import com.cartechiq.example.ui.theme.SampleAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            SampleAppTheme {
                MainScreen(this)
            }
        }
    }
}

@Preview(showBackground = true, device = "id:Nexus 7")
@Composable
fun DefaultPreview() {
    SampleAppTheme {
        MainScreen(context = LocalContext.current)
    }
}

@Composable
fun MainScreen(context: Context) {
    var postbackUrl by remember {
        mutableStateOf("")
    }
    var vin by remember {
        mutableStateOf("1GNSKCKC8GR112652")
    }
    var codes by remember {
        mutableStateOf(listOf("C0750"))
    }
    var code by remember {
        mutableStateOf("")
    }
    val runnerOutput = remember { mutableStateOf("") }
    val cartechIQService = CartechIQService(context);
    val coroutineScope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var dialogText by remember { mutableStateOf("No text provided") } ;
    var dialogTitle by remember { mutableStateOf("No title provided") } ;

    if (showDialog) {
        AlertDialog(
            onDismissRequest ={ showDialog = false },
            title = { Text(dialogTitle) },
            text = { Text(dialogText) },
            confirmButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Confirm")
                }
            }
        )
    }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        Text(text = "Postback URL", modifier = Modifier.padding(
            start= 10.dp, top = 10.dp, end = 10.dp))
        TextField(
            value= postbackUrl,
            onValueChange = {text ->
                postbackUrl = text
            })
        Text(text = "VIN", modifier = Modifier.padding(
            start= 10.dp, top = 10.dp, end = 10.dp))
        TextField(
            value= vin,
            onValueChange = {text ->
                vin = text
            })
        Text(text = "Codes",
            modifier = Modifier
                .padding(start= 10.dp, top = 20.dp),

            )
        LazyColumn(modifier = Modifier.padding(20.dp)) {
            items(codes.size) { index ->
                Text(text = codes[index])
            }
        }
        Row(modifier = Modifier.padding(10.dp)) {
            TextField(modifier = Modifier.width(150.dp),
                value= code,
                onValueChange = {text ->
                    code = text
                })
            Button(modifier = Modifier.padding(5.dp),
                onClick = {
                    if(code.isNotEmpty()) {
                        codes = codes + code
                        code = ""
                    }
                }) {
                Text(text = "Add")
            }
            Button(modifier = Modifier.padding(5.dp),
                onClick = {
                    codes = listOf<String>()
                }) {
                Text(text = "Clear")
            }
        }
        Spacer(modifier = Modifier.padding(50.dp))
        Button(
            modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
            onClick = {
                Log.d("Roli", "Group Lookup clicked")
                var validated = true;
                if(postbackUrl.isBlank()) {
                    dialogTitle = "Error"
                    dialogText = "Postback URL is required. Go to https://webhook.site/ to create a new one."
                    showDialog = true
                    validated = false
                }
                if(vin.isBlank()) {
                    dialogTitle = "Error"
                    dialogText = "VIN is invalid"
                    showDialog = true
                    validated = false;
                }
                if(codes.isEmpty()) {
                    dialogTitle = "Error"
                    dialogText = "One or more fault codes are required"
                    showDialog = true
                    validated = false;
                }
                if(validated) {
                    val request = GroupLookupRequest(
                        vin = vin,
                        location = Location(
                            lat = "40.712812",
                            lon = "-74.006012"
                        ),
                        faults = codes,
                        postbackUrl = postbackUrl,
                        showExtendedResults = true
                    )

                    // Launch on UI thread because JS runs on background thread
                    coroutineScope.launch(Dispatchers.Main) {
                        val groupResponse = cartechIQService.groupLookup(request)
                        Log.d("RETURN", Json.encodeToString(groupResponse))
                        dialogTitle = "Group Lookup Response"
                        dialogText = Json.encodeToString(groupResponse)
                        showDialog = true
                    }
                }
            }) {
            Text(text = "Group Lookup")
        }
        if (runnerOutput.value.isNotEmpty()) {
            Text(text = runnerOutput.value)
        }
    }
}