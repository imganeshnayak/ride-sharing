package dz.notacompany.riide

import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class TripDetailsFragment(private val tripID: String) : Fragment(R.layout.fragment_trip_details) {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var source: Source

    private lateinit var timeTv : TextView
    private lateinit var departureTv : TextView
    private lateinit var destinationTv : TextView
    private lateinit var driverUsernameTv : TextView
    private lateinit var seatsLeftTv : TextView
    private lateinit var priceTv : TextView

    private lateinit var backButton: Button
    private lateinit var deleteButton : Button
    private lateinit var joinButton : Button
    private lateinit var leaveButton : Button
    private lateinit var viewPassengersButton : Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth
        db = Firebase.firestore

        timeTv = requireView().findViewById(R.id.timeDetailsTv)
        departureTv = requireView().findViewById(R.id.departureDetailsTv)
        destinationTv = requireView().findViewById(R.id.destinationDetailsTv)
        driverUsernameTv = requireView().findViewById(R.id.driverUsernameDetailsTv)
        seatsLeftTv = requireView().findViewById(R.id.seatsLeftTv)
        priceTv = requireView().findViewById(R.id.priceDetailsTv)
        backButton = requireView().findViewById(R.id.backDetailsButton)
        deleteButton = requireView().findViewById(R.id.deleteDetailsButton)
        joinButton = requireView().findViewById(R.id.joinDetailsButton)
        leaveButton = requireView().findViewById(R.id.leaveDetailsButton)
        viewPassengersButton = requireView().findViewById(R.id.viewPassengersButton)

        viewPassengersButton.setOnClickListener {
            (activity as MainActivity).replaceCurrentFragment(DisplayPassengersFragment(tripID))
        }

        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        (activity as MainActivity).createLoadingDialog()

        var isDriver = false

        // Get the tripID that the user is currently in
        source = if ((activity as MainActivity).isOnline()) Source.DEFAULT else Source.CACHE
        db.collection("trips").document(tripID).get(source)
            .addOnSuccessListener { trip ->

                // Save the trip data to their variables
                // Since Date and Driver are Object fields, we cast them as HashMaps
                val tripData = trip.data
                val date = tripData?.get("date") as HashMap<*, *>
                val driver = tripData["driver"] as HashMap<*, *>
                val seatsLeft = tripData["seatsLeft"].toString().toInt()

                // Will add " (Full)" next to the number of taken seats if the trip is full
                var seatsFull = ""
                if (seatsLeft == 0) seatsFull = getString(R.string.full)

                // Check if the user is logged in
                if ((activity as MainActivity).isLoggedIn) {

                    // if user is the driver, show the delete button
                    if (driver["userID"]?.equals(auth.currentUser!!.uid)!!) {

                        isDriver = true
                        deleteButton.visibility = View.VISIBLE

                    } else {

                        // if user is in this trip, but not driver (user is passenger), show leave button
                        // Else (user is not in a trip), show Join button
                        if ((activity as MainActivity).isInTrip && ((activity as MainActivity).currentTripID == tripID)) {
                            leaveButton.visibility = View.VISIBLE
                        } else if ((!(activity as MainActivity).isInTrip) && (seatsLeft > 0)) {
                            joinButton.visibility = View.VISIBLE
                        }
                    }
                }

                (activity as MainActivity).displayTimeInTv(timeTv,
                    date["hour"].toString().toInt(),
                    date["minute"].toString().toInt(),
                    date["day"].toString().toInt(),
                    date["month"].toString().toInt(),
                    date["year"].toString().toInt())

                departureTv.text = tripData["departure"].toString()
                destinationTv.text = tripData["destination"].toString()

                // If the user is the driver, adds "(You)" next to their username
                var newDriverUsername = ""
                if (isDriver) {
                    newDriverUsername = driver["username"].toString().plus(getString(R.string.you))
                } else {
                    newDriverUsername = driver["username"].toString()
                }

                // This spannableString will make the driver's username underlined
                val usernameSpannableString = SpannableString(newDriverUsername)
                usernameSpannableString.setSpan(UnderlineSpan(), 0, usernameSpannableString.length, 0)
                driverUsernameTv.text = usernameSpannableString

                // Set listener to open driver's info when their username is clicked if the user is logged in
                driverUsernameTv.setOnClickListener {
                    if ((activity as MainActivity).isLoggedIn) {
                        (activity as MainActivity).replaceCurrentFragment(UserInfoFragment(driver["userID"].toString()))
                    } else {
                        Toast.makeText(
                            context, getString(R.string.not_logged_in_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                seatsLeftTv.text = "${(tripData["maxPassengers"].toString().toInt()) - seatsLeft}/${tripData["maxPassengers"]}".plus(seatsFull)
                priceTv.text = tripData["price"].toString().plus(" DZD")

                // Join button clicked
                // will add trip info to user document
                // then add user as a passenger in trip ("passengers" collection)
                joinButton.setOnClickListener {

                    // Check if user has filled in his contact info
                    if (!(activity as MainActivity).filledInfo) {
                        // Contact info missing

                        Toast.makeText(context,
                            getString(R.string.fill_contact_info),
                            Toast.LENGTH_SHORT).show()

                    } else {
                        // User has filled in his info

                        (activity as MainActivity).createLoadingDialog()

                        // New data to put in user document
                        val newUserData = hashMapOf(
                            "isInTrip" to true,
                            "currentTripID" to tripID
                        )

                        var tripAvailable = false

                        db.runTransaction { transaction ->

                            val userRef = db.collection("users").document(auth.currentUser!!.uid)
                            val tripRef = db.collection("trips").document(tripID)
                            val newPassengerRef = db.collection("trips").document(tripID).collection("passengers").document(auth.currentUser!!.uid)

                            // Recheck if the trip does indeed have seats left
                            val snapshot = transaction.get(tripRef)
                            val currentSeatsLeft = snapshot.data?.get("seatsLeft").toString().toInt()

                            if (currentSeatsLeft > 0) { // Trip does have seats left

                                tripAvailable = true

                                // Add the new data to user document
                                transaction.set(userRef, newUserData, SetOptions.merge())

                                // Update the number of seats left in the trip document
                                transaction.update(tripRef, "seatsLeft", currentSeatsLeft - 1)

                                // Add the user as a passenger in the "passengers" collection inside the trip document
                                transaction.set(newPassengerRef, hashMapOf("username" to (activity as MainActivity).username))

                            }

                        }.addOnCompleteListener {

                            if (tripAvailable) { // User joined the trip

                                // Cache the new variables
                                (activity as MainActivity).isInTrip = true
                                (activity as MainActivity).currentTripID = tripID

                                (activity as MainActivity).dismissLoadingDialog()

                                Toast.makeText(context,
                                    getString(R.string.trip_joined),
                                    Toast.LENGTH_SHORT).show()

                                (activity as MainActivity).navBar.selectedItemId =
                                    R.id.page_profile

                            } else { // User couldn't join the trip

                                (activity as MainActivity).dismissLoadingDialog()

                                Toast.makeText(context,
                                    getString(R.string.trip_recheck_error),
                                    Toast.LENGTH_SHORT).show()

                                (activity as MainActivity).navBar.selectedItemId =
                                    R.id.page_profile

                            }

                        }.addOnFailureListener {
                            (activity as MainActivity).dismissLoadingDialog()

                            Toast.makeText(context,
                                it.localizedMessage,
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                // Leave button clicked
                // will set user as no longer in trip
                // then remove user from the trip's "passengers" collection
                leaveButton.setOnClickListener {

                    (activity as MainActivity).createLoadingDialog()

                    db.runBatch { batch ->

                        val userRef = db.collection("users").document(auth.currentUser!!.uid)
                        val tripRef = db.collection("trips").document(tripID)
                        val passengerRef = db.collection("trips").document(tripID).collection("passengers").document(auth.currentUser!!.uid)

                        // Set user as not in a trip
                        batch.update(userRef, "isInTrip", false)

                        // Update the number of seats left in the trip document
                        batch.update(tripRef, "seatsLeft", seatsLeft + 1)

                        /// Delete user from the passengers SubCollection
                        batch.delete(passengerRef)

                    }.addOnCompleteListener {

                        // Cache variable
                        (activity as MainActivity).isInTrip = false

                        (activity as MainActivity).dismissLoadingDialog()

                        Toast.makeText(context,
                            getString(R.string.trip_left),
                            Toast.LENGTH_SHORT).show()

                        parentFragmentManager.popBackStack()

                    }.addOnFailureListener {
                        (activity as MainActivity).dismissLoadingDialog()

                        Toast.makeText(context,
                            it.localizedMessage,
                            Toast.LENGTH_SHORT).show()
                    }
                }

                // Delete button clicked
                // Display a warning, if user presses positive button, delete the trip from the DB
                // Prior to deleting, we have to flag driver and all passengers as no longer in a trip
                deleteButton.setOnClickListener {

                    // Display the warning
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.warning))
                        .setMessage(getString(R.string.trip_delete_warning))
                        .setNeutralButton(getString(R.string.cancel)) { dialog, _ ->
                            // User clicks cancel

                            dialog.dismiss()
                        }
                        .setPositiveButton(getString(R.string.delete)) { dialog, _ ->
                            // User clicks delete

                            dialog.dismiss()

                            (activity as MainActivity).createLoadingDialog()

                            // Fetch all passengers in the trip
                            db.collection("trips").document(tripID)
                                .collection("passengers").get(Source.SERVER)
                                .addOnSuccessListener { passengers ->

                                    db.runBatch { batch ->

                                        // Cycle through the passengers to kick them from the trip
                                        for (passenger in passengers) {

                                            // Set each passenger as no longer in a trip
                                            batch.update(db.collection("users").document(passenger.id), "isInTrip", false)

                                            // Delete all passenger documents from the passengers SubCollection
                                            batch.delete(db.collection("trips").document(tripID).collection("passengers").document(passenger.id))
                                        }

                                        // Flag driver as no longer in a trip
                                        batch.update(db.collection("users").document(driver["userID"].toString()), "isInTrip", false)

                                        // Delete this trip's document from the DB
                                        batch.delete(db.collection("trips").document(tripID))

                                    }.addOnCompleteListener {
                                        (activity as MainActivity).isInTrip = false

                                        (activity as MainActivity).dismissLoadingDialog()

                                        Toast.makeText(context,
                                            getString(R.string.trip_deleted),
                                            Toast.LENGTH_SHORT).show()

                                        parentFragmentManager.popBackStack()
                                    }
                                }
                                .addOnFailureListener {
                                    // Could not fetch passengers data

                                    (activity as MainActivity).dismissLoadingDialog()

                                    Toast.makeText(
                                        context,
                                        it.localizedMessage,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                        .show()
                }

                (activity as MainActivity).dismissLoadingDialog()
            }
            .addOnFailureListener {
                // Cannot access trip data

                (activity as MainActivity).dismissLoadingDialog()

                Toast.makeText(
                    context,
                    it.localizedMessage,
                    Toast.LENGTH_SHORT
                ).show()
            }

    }
}