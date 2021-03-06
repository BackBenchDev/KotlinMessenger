 package com.example.kotlinmessenger.registerlogin

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.example.kotlinmessenger.message.HomeScreen
import com.example.kotlinmessenger.models.User
import com.example.kotlinmessenger.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import java.util.*


 class RegisterActivity : AppCompatActivity() {

     //[START declare_auth]
     private lateinit var auth: FirebaseAuth
     //[STOP declare_auth]

     // [Declare variable of ViewBinding for activity_main.xml layout file]
     private lateinit var binding: ActivityRegisterBinding

     public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //[initialise_auth]
        auth = Firebase.auth

        //[inflate the ActivityMainBinding class file]
        binding = ActivityRegisterBinding.inflate(layoutInflater)

        //[select the content view to the inflated ActivityMainBinding file]
        setContentView(binding.root)

        //[setting up to take inputs and do user creation as soon a registration button is clicked]
        binding.registerButtonRegistration.setOnClickListener {

            //[START account creation process]
            createAccount()
            //[END account creation process]

        }

        // [setting up to change activity to LoginActivity]
         binding.signInLink.setOnClickListener {
             //[ function call to start_login_activity ]
             changeActivityTo("loginActivity")

         }
         // [stop of changing activity to LoginActivity]

         // [setting up to create user profile image]
         binding.selectImageButtonRegistration.setOnClickListener {

             selectImage()

         }
     }

     // [function to check user condition on app onStart state]
     public override fun onStart() {
         super.onStart()

         // [to check whether there is some user already logged in the app]
         val currentUser = auth.currentUser
         if( currentUser != null ) {
             changeActivityTo("homeScreenActivity")
         }
     }


     // [START function to create account]
     private fun createAccount() {

         // [initialize email, password and user-name fields]
         val emailValue = binding.emailaddressEdittextRegistration.text.toString()
         val userNameValue = binding.usernameEdittextRegistration.text.toString()
         val passwordValue = binding.passwordEdittextRegistration.text.toString()

         // [checking whether the fields i.e. email and password is empty or and prevent from crashing]
         if(emailValue.isEmpty() || passwordValue.isEmpty()){
             Toast.makeText(applicationContext,"Please enter the details above",Toast.LENGTH_SHORT).show()
             return
         }

         //[ specify conditions for writing password ]
         if(passwordValue.length < 6){
             Toast.makeText(applicationContext,"Please enter a password with minimum character length 6",Toast.LENGTH_SHORT).show()
             return
         }

         // check to make sure that user selects profile image
         if(binding.selectedImageViewRegistration.drawable == null) {
             Toast.makeText(baseContext,"Please select profile image",Toast.LENGTH_SHORT).show()
             return
         }

         // [START creating new account]
         auth.createUserWithEmailAndPassword(emailValue, passwordValue)
                 .addOnCompleteListener(this) { task ->
                     if (task.isSuccessful) {

                         // [Sign in success]
                         Log.d("RegisterActivity", "createUserWithEmail:success")

                         // [logging the data entered in email, password and username field in RegisterActivity]
                         Log.d("RegisterActivity","Username: $userNameValue")
                         Log.d("RegisterActivity", "Email is: $emailValue")
                         Log.d("RegisterActivity", "Password is: $passwordValue")

                         uploadImageToFirebaseStorage()
                         Toast.makeText(baseContext,"Account successfully created",Toast.LENGTH_SHORT).show()

                     } else {
                         // [If sign in fails, display a message to the user.]
                         Log.d("RegisterActivity", "createUserWithEmail:failure", task.exception)
                         Toast.makeText(baseContext, "Failed to create an account",Toast.LENGTH_SHORT).show()

                     }
                 }
         // [STOP creating new account]


     }
     // [STOP function to create account]

     // [Function to select image for user's profile]
     private fun selectImage(){
         Log.d("RegisterActivity","Try to show image selector")

         // [creating intent to run photos app from the device's local storage to select image]
         val intentToRunImageActivity = Intent(Intent.ACTION_PICK)
         intentToRunImageActivity.type = "image/*"
         startActivityForResult(intentToRunImageActivity,0)
     }

     // [Declare URI data for the image selected]
     private var selectedImageUri: Uri? = null

     // [START function to display result after getting result from intent_to_run_imageActivity]
     override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
         super.onActivityResult(requestCode, resultCode, data)

         if(requestCode == 0 && resultCode == Activity.RESULT_OK && data != null ){

             // [check what the input image was]
             Log.d("RegisterActivity","An image was selected")

             // [getting uri from the "data" passed in the onActivityResult function]
             selectedImageUri = data.data

             // [getting bitmap image data from uri]
             val bitmapImageSelected = MediaStore.Images.Media.getBitmap(this.contentResolver, selectedImageUri)

             // [projecting the image selected on the circleImageView]
             binding.selectedImageViewRegistration.setImageBitmap(bitmapImageSelected)

             // [setting the transparency of the "select image" button to full]
             binding.selectImageButtonRegistration.alpha = 0f

         }
     }
     // [STOP function onActivityResult]

     // [Function to upload image to Firebase Database]
     private fun uploadImageToFirebaseStorage(){

         // [declare a random filename]
         val filename = UUID.randomUUID().toString()
         val ref = FirebaseStorage.getInstance().getReference("/images/$filename")
         ref.putFile(selectedImageUri!!)
                 .addOnSuccessListener {
                     Log.d("RegisterActivity","Successfully uploaded image: ${it.metadata?.path}")

                     ref.downloadUrl.addOnSuccessListener { link ->
                         Log.d("RegisterActivity","File Location: $link")

                         saveUserToFirebaseDatabase(link.toString())
                     }
                 }

     }

     // [Function to upload user to User's info to FirebaseDatabase]
     private fun saveUserToFirebaseDatabase(profileImageUrl: String){
         val uid = FirebaseAuth.getInstance().uid ?: ""
         val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")

         Log.d("RegisterActivity", "Upload the data related to user somehow")
         // [uploads the data to Real time database]
         val user = User(uid, binding.usernameEdittextRegistration.text.toString(), profileImageUrl)
         ref.setValue(user)
                 .addOnSuccessListener {
                     Log.d("RegisterActivity","Current user: $uid\n username:${binding.usernameEdittextRegistration.text}\n has been save to Firebase Database")
                     Toast.makeText(baseContext,"Signing in",Toast.LENGTH_SHORT).show()
                     changeActivityTo("homeScreenActivity")
                 }
                 .addOnFailureListener {
                     Log.d("RegisterActivity","Failed to add user to Firebase Database")
                 }

     }

     // [Function to change between activities]
     private fun changeActivityTo(activity: String) {

         when(activity){
             "loginActivity" -> {
                 //[update log to reflect request ot change activity]
                 Log.d("RegisterActivity", "Try to show login activity")
                 Toast.makeText(applicationContext, "Redirecting", Toast.LENGTH_SHORT).show()

                 // [making Intent to change from RegisterActivity to LoginActivity]
                 val intentToRunLoginActivity = Intent(this, LoginActivity::class.java)
                 startActivity(intentToRunLoginActivity)
             }
             "homeScreenActivity" -> {
                 val intentToRunHomeScreenActivity = Intent(this, HomeScreen::class.java)
                 // [this statement is to clear other activities of the stack]
                 intentToRunHomeScreenActivity.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                 startActivity(intentToRunHomeScreenActivity)
             }


         }
     }

     



}

