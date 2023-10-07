// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.ButtonDefaults.buttonColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.mongodb.BasicDBObject
import com.mongodb.client.FindIterable
import com.mongodb.client.MongoCollection
import com.toxicbakery.bcrypt.Bcrypt
import org.bson.Document
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import org.litote.kmongo.Id
import org.litote.kmongo.newId
import java.math.BigDecimal
import io.github.cdimascio.dotenv.dotenv


class Location(val type: String, val coordinates: Array<BigDecimal>)
class Restaurant(val _id: Id<String> = newId(), val name: String)

val CYBER_GRAPE = Color(0xff63458A)
val ROYAL_GRAPE = Color(0xff7E5A9B)
val MAXIMUM_YELLOW_RED = Color(0xffE3B23C)
val BABY_POWDER = Color(0xffFBFEFB)

val dotenv = dotenv()

@Composable
fun tableScreen(database: DatabaseConnection, something: FindIterable<Restaurant>) {
    val showButtons = remember { mutableStateOf(true) }
    val restaurant = remember { mutableStateOf(something.first()) }
    val tableData = (something).mapIndexed { index, item ->
        index to item
    }
    Column(
        Modifier.padding(100.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if(showButtons.value) {
            Text("Choose your restaurant and register.", color = ROYAL_GRAPE, fontSize = 20.sp)
            tableData.forEach {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center){
                    Button(onClick = ({
                        showButtons.value = false
                        restaurant.value = it.second
                    }),
                        Modifier.size(width = 300.dp, height = 70.dp).padding(5.dp),
                        colors = buttonColors(
                            backgroundColor = ROYAL_GRAPE
                        )) {
                        Text(it.second.name, color = BABY_POWDER)
                    }
                }
            }
        }
        if(!showButtons.value) {
            oldRestaurantForm(database, restaurant.value)
        }
    }
}

@Composable
@Preview
fun app(database: DatabaseConnection) {
    val showButtons = remember { mutableStateOf(true) }
    val new = remember { mutableStateOf(false) }
    val updateOld = remember { mutableStateOf(false) }
    Column {
        TopAppBar(
            elevation = 4.dp,
            title = {
                Text("Hello Restaurant!", color = BABY_POWDER)
            },
            backgroundColor = CYBER_GRAPE,
            navigationIcon = {
                IconButton(onClick = {
                    showButtons.value = true
                    new.value = false
                    updateOld.value = false
                }) {
                    Icon(Icons.Filled.Home, null, tint = BABY_POWDER)
                }
            })
    }
    AnimatedVisibility(visible = showButtons.value, enter = fadeIn(), exit = fadeOut()) {
        Text(text = buildAnnotatedString {
            withStyle(style = SpanStyle(color = ROYAL_GRAPE)) {
                append("Welcome to ")
            }
            withStyle(style = SpanStyle(color = MAXIMUM_YELLOW_RED)) {
                append("Eatical")
            }
            withStyle(style = SpanStyle(color = ROYAL_GRAPE)) {
                append(
                    ". Join our community and help us save food.\n" +
                            "If you are new in Maribor and haven't contacted us yet, make an account from scratch, " +
                            "otherwise, register with only username and password."
                )
            }
        },
            modifier = Modifier.padding(horizontal = 100.dp, vertical = 150.dp),
            fontStyle = FontStyle.Italic
        )
        Button(onClick = ({
            new.value = true
            showButtons.value = false
        }),
            Modifier.padding(horizontal = 100.dp, vertical = 250.dp), enabled = true,
            colors = buttonColors(
                backgroundColor = ROYAL_GRAPE
            )) {
            Text("Create new restaurant", color = BABY_POWDER)
        }
        Button(onClick = {
            updateOld.value = true
            showButtons.value = false
        },
            Modifier.padding(horizontal = 300.dp, vertical = 250.dp),
            colors = buttonColors(
                backgroundColor = ROYAL_GRAPE
            )) {
            Text("Register", color = BABY_POWDER)
        }
    }
    if(new.value){
        newRestaurantForm(database)
    }else if(updateOld.value){
        val restaurants:MongoCollection<Restaurant> = database.getRestaurants()
        tableScreen(database, restaurants.find())
    }
}

@Composable
fun newRestaurantForm(database: DatabaseConnection){
    val name = remember { mutableStateOf(TextFieldValue()) }
    val username = remember { mutableStateOf(TextFieldValue()) }
    val password = remember { mutableStateOf(TextFieldValue()) }
    val repeatPassword = remember { mutableStateOf(TextFieldValue()) }
    val latitude = remember { mutableStateOf(TextFieldValue()) }
    val longitude = remember { mutableStateOf(TextFieldValue()) }
    val hash = Bcrypt.hash(repeatPassword.value.text, 10)
    val address = remember { mutableStateOf(TextFieldValue()) }
    val error = remember { mutableStateOf("") }
    val latitudeFormatChecker = remember { mutableStateOf(false) }
    val longitudeFormatChecker = remember { mutableStateOf(false) }
    val latitudeValueChecker = remember { mutableStateOf(false) }
    val longitudeValueChecker = remember { mutableStateOf(false) }
    val registered = remember { mutableStateOf(false) }

    AnimatedVisibility(visible = !registered.value, enter = fadeIn(), exit = fadeOut()){
        Column(Modifier.padding(100.dp).verticalScroll(rememberScrollState())){
            inputField("name", name)
            inputField("username", username)
            Row(Modifier.padding(bottom = 5.dp)){
                TextField(
                    value = password.value,
                    label = { Text("password", color = CYBER_GRAPE) },
                    onValueChange = { password.value = it },
                    visualTransformation = PasswordVisualTransformation()
                )
            }
            Row(Modifier.padding(bottom = 5.dp)){
                TextField(
                    value = repeatPassword.value,
                    label = { Text("repeat password", color = CYBER_GRAPE) },
                    onValueChange = { repeatPassword.value = it },
                    visualTransformation = PasswordVisualTransformation()
                )
            }
            inputField("address", address)
            inputField("latitude", latitude)
            inputField("longitude", longitude)
            Button(onClick = ({
                latitudeFormatChecker.value = latitude.value.text.matches("-?\\d+(\\.\\d+)?".toRegex())
                longitudeFormatChecker.value = latitude.value.text.matches("-?\\d+(\\.\\d+)?".toRegex())
                if(latitude.value.text != "" && latitudeFormatChecker.value)
                    latitudeValueChecker.value = BigDecimal(latitude.value.text).compareTo(BigDecimal(-91)) == 1 &&
                            BigDecimal(latitude.value.text).compareTo(BigDecimal(91)) == -1
                if(longitude.value.text != "" && longitudeFormatChecker.value)
                    longitudeValueChecker.value = BigDecimal(longitude.value.text).compareTo(BigDecimal(-181)) == 1 &&
                            BigDecimal(longitude.value.text).compareTo(BigDecimal(181)) == -1
                if(name.value.text == "")
                    error.value = "Please enter name."
                else if(username.value.text == "")
                    error.value = "Please enter username."
                else if(username.value.text.length < 3)
                    error.value = "Please enter username with at least 3 characters."
                else if(password.value.text == "")
                    error.value = "Please enter password."
                else if(password.value.text.length < 6)
                    error.value = "Please enter password with at least 6 characters."
                else if(repeatPassword.value.text == "")
                    error.value = "Please repeat password."
                else if(password.value.text != repeatPassword.value.text)
                    error.value = "Passwords do not match."
                else if(address.value.text == "")
                    error.value = "Please enter address."
                else if(latitude.value.text == "")
                    error.value = "Please enter latitude."
                else if(longitude.value.text == "")
                    error.value = "Please enter longitude."
                else if(!latitudeFormatChecker.value || !longitudeFormatChecker.value)
                    error.value = "Please enter correct latitude format and longitude format. Example:46.1578."
                else if(!latitudeValueChecker.value || !longitudeValueChecker.value)
                    error.value = "Latitude must be between -90 and 90. Longitude must be between -180 and 180."
                else {
                    val document = Document()
                    error.value = ""
                    registered.value = true
                    val point = Location("Point", arrayOf(latitude.value.text.toBigDecimal(), longitude.value.text.toBigDecimal()))
                    document.put("name", name.value.text)
                    document.put("address", address.value.text)
                    document.put("location", point)
                    document.put("username", username.value.text)
                    document.put("password", hash)
                    database.insertRestaurant(document)
                }
            }),
                colors = buttonColors(
                    backgroundColor = ROYAL_GRAPE
                )){
                Text("Register", color = BABY_POWDER)
            }
            Text(
                text = error.value,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = CYBER_GRAPE,
                fontSize = 30.sp
            )
        }
    }
    AnimatedVisibility(visible = registered.value, enter = fadeIn(), exit = fadeOut()){
        Text(
            text = "You have successfully created an account. Open our app to continue.",
            modifier = Modifier.padding(horizontal = 100.dp, vertical = 150.dp),
            fontWeight = FontWeight.Bold,
            color = CYBER_GRAPE,
            fontSize = 30.sp
        )
    }
}

@Composable
fun oldRestaurantForm(database: DatabaseConnection, restaurant: Restaurant){
    val username = remember { mutableStateOf(TextFieldValue()) }
    val password = remember { mutableStateOf(TextFieldValue()) }
    val repeatPassword = remember { mutableStateOf(TextFieldValue()) }
    val hash = Bcrypt.hash(repeatPassword.value.text, 10)
    val registered = remember { mutableStateOf(false) }

    AnimatedVisibility(visible = !registered.value, enter = fadeIn(), exit = fadeOut()){
        Column(Modifier.padding(100.dp)){
            inputField("username", username)
            Row(Modifier.padding(bottom = 5.dp)){
                TextField(
                    value = password.value,
                    label = { Text("password", color = CYBER_GRAPE) },
                    onValueChange = { password.value = it },
                    visualTransformation = PasswordVisualTransformation()
                )
            }
            Row(Modifier.padding(bottom = 5.dp)){
                TextField(
                    value = repeatPassword.value,
                    label = { Text("repeat password", color = CYBER_GRAPE) },
                    onValueChange = { repeatPassword.value = it },
                    visualTransformation = PasswordVisualTransformation()
                )
            }
            val error = remember { mutableStateOf("") }
            Button(onClick = ({
                if(username.value.text == "")
                    error.value = "Please enter username."
                else if(username.value.text.length < 3)
                    error.value = "Please enter username with at least 3 characters."
                else if(password.value.text == "")
                    error.value = "Please enter password."
                else if(password.value.text.length < 6)
                    error.value = "Please enter password with at least 6 characters."
                else if(repeatPassword.value.text == "")
                    error.value = "Please repeat password."
                else if(password.value.text != repeatPassword.value.text)
                    error.value = "Passwords do not match."
                else {
                    error.value = ""
                    registered.value = true
                    val document = BasicDBObject()
                    document.put("username", username.value.text)
                    document.put("password", hash)
                    database.updateRestaurant(document, restaurant._id)
                }
            }),
                colors = buttonColors(
                    backgroundColor = ROYAL_GRAPE
                )){
                Text("Register", color = BABY_POWDER)
            }
            Text(
                text = error.value,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = CYBER_GRAPE,
                fontSize = 30.sp
            )
        }
    }
    AnimatedVisibility(visible = registered.value, enter = fadeIn(), exit = fadeOut()){
        Text(
            text = "You have successfully registered. Open our app to continue.",
            modifier = Modifier.padding(horizontal = 100.dp, vertical = 150.dp),
            fontWeight = FontWeight.Bold,
            color = CYBER_GRAPE,
            fontSize = 30.sp
        )
    }
}

@Composable
fun inputField(type: String, state: MutableState<TextFieldValue>){
    Row(Modifier.padding(bottom = 5.dp)){
        TextField(
            value = state.value,
            label = { Text(type, color = CYBER_GRAPE) },
            onValueChange = { state.value = it }
        )
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Eatical") {
        val database = DatabaseConnection(
            dotenv.get("MONGO_DB_URL"),
            dotenv.get("MONGO_DB_NAME")
        )
        app(database)
    }
}