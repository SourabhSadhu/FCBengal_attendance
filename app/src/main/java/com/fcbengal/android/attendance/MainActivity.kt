package com.fcbengal.android.attendance

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import com.crashlytics.android.Crashlytics
import com.fcbengal.android.attendance.entity.Config
import com.fcbengal.android.attendance.utils.DatabaseUtil
import com.fcbengal.android.attendance.utils.KLoadingSpin
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseError
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {

    private val TAG = this::class.java.simpleName
    //Authentication
    // Firebase instance variables
    private var mFirebaseAuth: FirebaseAuth? = null
    private var mFirebaseUser: FirebaseUser? = null
    private var mGoogleApiClient: GoogleApiClient? = null
    private lateinit var loadingSpinner : KLoadingSpin
    private var userRoles = ""
    private lateinit var contentMain : ConstraintLayout
    private var config = Config()
    private var userName = ""
    private lateinit var firebaseAnalytics : FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        loadingSpinner = findViewById(R.id.KLoadingSpin)
        contentMain = findViewById(R.id.content_main)
        mFirebaseAuth = FirebaseAuth.getInstance()
        mFirebaseUser = mFirebaseAuth?.currentUser
        mGoogleApiClient = GoogleApiClient.Builder(this)
            .enableAutoManage(this) { connectionResult ->
                Log.d(TAG, "onConnectionFailed:$connectionResult")
                Toast.makeText(this@MainActivity, "Google Play Services error.", Toast.LENGTH_SHORT).show()
            }
            .addApi(Auth.GOOGLE_SIGN_IN_API)
            .build()

        if (mFirebaseUser != null) {
            userName = mFirebaseUser?.displayName.toString()
            Toast.makeText(this@MainActivity, "Welcome $userName", Toast.LENGTH_SHORT).show()
//            if (mFirebaseUser?.getPhotoUrl() != null) {
//                val mPhotoUrl = mFirebaseUser?.getPhotoUrl()!!.toString()
//            }
            val gMail = mFirebaseUser?.email
            if(null != gMail) {

                Crashlytics.setUserName(userName)
                Crashlytics.setUserEmail(gMail)
                firebaseAnalytics = FirebaseAnalytics.getInstance(this@MainActivity)
                firebaseAnalytics.setUserId(gMail)

                showLoader()
                DatabaseUtil.loadConfig(gMail, object : DatabaseUtil.OnDataCompletedListener {
                    override fun onCancelled(error: DatabaseError) {
                        stopLoader(false,error.message)
                    }

                    override fun onDataChange(data: Any) {
                        stopLoader()
                        config = data as Config
                        if(config.auth) {
                            userRoles = config.role
                        }else{
                            Toast.makeText(this@MainActivity, "Temporarily blocked", LENGTH_SHORT).show()
                        }
                    }
                })
            }else{
                Toast.makeText(this,"Unauthorized Entry", Toast.LENGTH_LONG).show()
                mFirebaseAuth?.signOut()
                Auth.GoogleSignInApi.signOut(mGoogleApiClient)
                startActivity(Intent(this, SignInActivity::class.java))
                finish()
            }
        } else {
            // Not signed in, launch the Sign In activity
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        DatabaseUtil.checkDBStatus(object : DatabaseUtil.OnDatabaseConnectivityListener{
            override fun onStatusUpdated(key: String, value: String) {
                Snackbar.make(contentMain, value, Snackbar.LENGTH_SHORT).show()
            }
        })

        card_view_create_attendance_entry.setOnClickListener {
            if(userRoles.contains(getString(R.string.admin)) || userRoles.contains(getString(
                    R.string.create_attendance_data
                ))){
                startActivity(Intent(this@MainActivity, AttendanceSetupActivity::class.java))
            } else {
                Toast.makeText(this@MainActivity, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
            }
        }
        card_view_create_attendance_report.setOnClickListener {
            if(userRoles.contains(getString(R.string.admin)) || userRoles.contains(getString(
                    R.string.create_attendance_report
                ))){
                val reportIntent = Intent(this@MainActivity, AttendanceReportActivity::class.java)
                reportIntent.putExtra(getString(R.string.email_to_address_key), config.email)
                reportIntent.putExtra(getString(R.string.user_name_key), userName)
                startActivity(reportIntent)
            } else {
                Toast.makeText(this@MainActivity, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
            }
        }
        card_view_create_metadata.setOnClickListener {
            if(userRoles.contains(getString(R.string.admin)) || userRoles.contains(getString(
                    R.string.create_metadata
                ))){
                startActivity(Intent(this@MainActivity, MetaDataActivity::class.java))
            } else {
                Toast.makeText(this@MainActivity, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBackPressed() {
        if(loadingSpinner.mIsVisible){
            stopLoader()
        }else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                mFirebaseAuth?.signOut()
                Auth.GoogleSignInApi.signOut(mGoogleApiClient)
                startActivity(Intent(this, SignInActivity::class.java))
                finish()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLoader(){
        card_view_create_attendance_entry.visibility = View.GONE
        card_view_create_attendance_report.visibility = View.GONE
        card_view_create_metadata.visibility = View.GONE
        loadingSpinner.setIsVisible(true)
        loadingSpinner.startAnimation()
    }

    private fun stopLoader(isCompletedSuccess : Boolean = true, msg : String = ""){
        card_view_create_attendance_entry.visibility = View.VISIBLE
        card_view_create_attendance_report.visibility = View.VISIBLE
        card_view_create_metadata.visibility = View.VISIBLE
        loadingSpinner.setIsVisible(false)
        loadingSpinner.stopAnimation()
        if(!isCompletedSuccess && !TextUtils.isEmpty(msg)) {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

}
