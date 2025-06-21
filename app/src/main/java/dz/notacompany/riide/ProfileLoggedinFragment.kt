package dz.notacompany.riide

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ProfileLoggedinFragment : Fragment(R.layout.fragment_profile_loggedin) {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var source: Source

    private lateinit var logoutButton : Button
    private lateinit var tripDetailsButton : Button
    private lateinit var openProfileButton : Button
    private lateinit var openGithubButton : Button
    private lateinit var helpButton : Button

    private lateinit var isInTripTv : TextView
    private lateinit var welcomeTv : TextView
    private lateinit var introTextTv : TextView
    private lateinit var separator0 : TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth
        db = Firebase.firestore

        logoutButton = requireView().findViewById(R.id.logoutButton)
        tripDetailsButton = requireView().findViewById(R.id.tripDetailsButton)
        openProfileButton = requireView().findViewById(R.id.openProfileInfoButton)
        openGithubButton = requireView().findViewById(R.id.githubButton)
        helpButton = requireView().findViewById(R.id.helpButton)
        isInTripTv = requireView().findViewById(R.id.isInTripTv)
        welcomeTv = requireView().findViewById(R.id.welcomeProfileTv)
        separator0 = requireView().findViewById(R.id.separator0)
        introTextTv = requireView().findViewById(R.id.profileIntroTextTv)

        // ----- Rescale some stuff if screen is too short --------
        val displayMetrics = resources.displayMetrics
        val displayHeight = displayMetrics.heightPixels / displayMetrics.density
        // val displayWidth = displayMetrics.widthPixels / displayMetrics.density

        if (displayHeight < (activity as MainActivity).minimumHeightDensity) {
            separator0.visibility = View.GONE
            welcomeTv.textSize = 24f
            introTextTv.textSize = 15f
        }
        // ---------------------------------------------------------

        welcomeTv.text = "${getString(R.string.welcome)}${(activity as MainActivity).username} !"

        // Display the trip user state from the cached variable
        if ((activity as MainActivity).isInTrip) {
            isInTripTv.textAlignment = TextView.TEXT_ALIGNMENT_TEXT_START
            isInTripTv.text = getString(R.string.current_trip_details)
            tripDetailsButton.visibility = View.VISIBLE
        } else {
            isInTripTv.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            isInTripTv.text = getString(R.string.no_trip_join)
            tripDetailsButton.visibility = View.GONE
        }

        openGithubButton.setOnClickListener {
            val openURL = Intent(Intent.ACTION_VIEW)
            openURL.data = Uri.parse("https://github.com/50t0r25/RiiDE-prototype")
            startActivity(openURL)
        }

        openProfileButton.setOnClickListener {
            // Will open the current user's profile info
            (activity as MainActivity).replaceCurrentFragment(UserInfoFragment(auth.currentUser!!.uid))
        }

        // Check the trip user state from the database
        // then add a button listener if user is in a trip
        source = if ((activity as MainActivity).isOnline()) Source.DEFAULT else Source.CACHE
        db.collection("users").document(auth.currentUser!!.uid).get(source)
            .addOnSuccessListener { user ->
                if (user.data?.get("isInTrip").toString().toBoolean()) {
                    // User is in a trip
                    (activity as MainActivity).isInTrip = true
                    isInTripTv.textAlignment = TextView.TEXT_ALIGNMENT_TEXT_START
                    isInTripTv.text = getString(R.string.current_trip_details)
                    tripDetailsButton.visibility = View.VISIBLE

                    tripDetailsButton.setOnClickListener {
                        (activity as MainActivity).replaceCurrentFragment(TripDetailsFragment(user.data?.get("currentTripID").toString()))
                    }
                } else {
                    // User isn't currently in a trip
                    (activity as MainActivity).isInTrip = false
                    isInTripTv.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                    isInTripTv.text = getString(R.string.no_trip_join)
                    tripDetailsButton.visibility = View.GONE
                }

            }
            .addOnFailureListener {
                Toast.makeText(context,
                    it.localizedMessage,
                    Toast.LENGTH_SHORT).show()
            }

        logoutButton.setOnClickListener {
            // Logout the user, refresh the main activity
            // then load the new non-loggedIn fragment
            FirebaseAuth.getInstance().signOut()
            (activity as MainActivity).refreshMainActivity()
            (activity as MainActivity).setCurrentFragment(ProfileFragment())
        }

        helpButton.setOnClickListener {

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.what_is_ride))
                .setMessage(getString(R.string.app_description))
                .setPositiveButton(getString(R.string.close), null)
                .show()
        }
    }
}