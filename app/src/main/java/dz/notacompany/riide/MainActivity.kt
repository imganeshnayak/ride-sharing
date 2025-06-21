package dz.notacompany.riide

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import java.util.concurrent.LinkedBlockingQueue

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    lateinit var navBar: BottomNavigationView

    lateinit var username : String
    lateinit var userEmail : String
    var isInTrip = false
    var filledInfo = false
    lateinit var currentTripID : String

    lateinit var departure : String
    lateinit var destination : String

    var isLoggedIn = false

    private lateinit var fragmentProfile : Fragment
    private var fragmentHome = HomeFragment()
    private lateinit var loading: AlertDialog

    val minimumHeightDensity = 700

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = Firebase.auth
        db = Firebase.firestore

        navBar = findViewById(R.id.bottom_navigation)

        refreshMainActivity()

        setCurrentFragment(fragmentHome) // Defaults view to home fragment

        // Nav bar clicks
        navBar.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.page_home -> {
                    Log.i("NavBar","Home pressed")
                    setCurrentFragment(fragmentHome)
                    true
                }
                R.id.page_profile -> {
                    Log.i("NavBar","Profile pressed")
                    setCurrentFragment(fragmentProfile)
                    true
                }
                else -> false
            }
        }
    }

    // Replaces fragment while adding to backstack
    fun replaceCurrentFragment(fragment: Fragment) =
        supportFragmentManager.beginTransaction().apply {
            setCustomAnimations(
                R.anim.explode_in,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.fade_out
            )
            replace(R.id.flFragment, fragment)
            addToBackStack(null)
            commit()
        }

    // Replaces fragment while CLEARING the backstack
    fun setCurrentFragment(fragment: Fragment) =
        supportFragmentManager.beginTransaction().apply {
            setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out
            )
            replace(R.id.flFragment, fragment)

            // This somehow clears the back stack
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            commit()
        }

    fun createLoadingDialog() {
        loading = MaterialAlertDialogBuilder(this)
            .setView(this.layoutInflater.inflate(R.layout.dialog_loading, null))
            .setCancelable(false)
            .create()
        loading.show()
    }

    fun dismissLoadingDialog() {
        loading.dismiss()
    }

    fun refreshMainActivity() {

        // Check if user is logged in
        val currentUser = auth.currentUser
        if(currentUser != null){

            // User logged in, set the profile fragment to be the logged in one
            fragmentProfile = ProfileLoggedinFragment()
            isLoggedIn = true

            // Set database source as cache for faster and offline access
            // Then fetch the user data from the DB
            db.collection("users").document(currentUser.uid).get(Source.CACHE)
                .addOnSuccessListener { user ->

                    username = user.data!!["username"].toString()
                    userEmail = user.data!!["email"].toString()
                    isInTrip = user.data!!["isInTrip"].toString().toBoolean()
                    filledInfo = user.data!!["filledInfo"].toString().toBoolean()
                    if (isInTrip) currentTripID = user.data!!["currentTripID"].toString()

                }
                .addOnFailureListener {

                    Toast.makeText(this,
                        it.localizedMessage,
                        Toast.LENGTH_SHORT).show()
                }
        } else {
            // User isn't logged in, set profile fragment to the default one
            fragmentProfile = ProfileFragment()
            isLoggedIn = false
        }
    }

    // Turns an Integer into a 2 digit String (for correct time display)
    private fun intDoubleDigit(int : Int) : String {
        var newInt = int.toString()
        if (newInt.length == 1) {
            newInt = "0$newInt"
        }
        return newInt
    }

    // Displays time and date in a TextView
    fun displayTimeInTv(tv : TextView, fHour : Int, fMinute : Int, fDay : Int, fMonth : Int, fYear : Int) {
        tv.text = "${getString(R.string.time)} ${intDoubleDigit(fHour)}:${intDoubleDigit(fMinute)}\n${getString(R.string.date)} ${intDoubleDigit(fDay)}/${intDoubleDigit(fMonth)}/$fYear"
    }

    // Formats Date (dd/mm/yyyy) to a string
    // Used to display date in recyclerView elements
    fun formatDate(fDay : Int, fMonth: Int, fYear: Int) : String {
        return "${intDoubleDigit(fDay)}/${intDoubleDigit(fMonth)}/$fYear"
    }

    // Function checks if user has internet access
    fun isOnline(): Boolean {
        val queue = LinkedBlockingQueue<Boolean>()

        // Start a thread to run check on a non-UI thread
        // prevents freezing on networks with no internet
        Thread {
            try {
                val timeoutMs = 1500
                val sock = Socket()
                val sockaddr: SocketAddress = InetSocketAddress("8.8.8.8", 53)
                sock.connect(sockaddr, timeoutMs)
                sock.close()
                queue.add(true)
            } catch (e: IOException) {
                queue.add(false)
            }
        }.start()
        return queue.take()
    }
}