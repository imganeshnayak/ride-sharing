package dz.notacompany.riide

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class UserInfoFragment(private val userID: String) : Fragment(R.layout.fragment_user_info) {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var source: Source

    private lateinit var username : TextView
    private lateinit var email : TextView
    private lateinit var infoTitle : TextView
    private lateinit var infoTv : TextView
    private lateinit var ratingTv : TextView
    private lateinit var bigTitleTv : TextView
    private lateinit var infoTextField : TextInputLayout
    private lateinit var infoEditText : TextInputEditText

    private lateinit var ratingBar : RatingBar
    private lateinit var infoScrollView : ScrollView

    private lateinit var backButton : Button
    private lateinit var editSaveButton : Button
    private lateinit var submitRatingButton : Button
    private lateinit var cancelEditButton : Button

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth
        db = Firebase.firestore

        username = requireView().findViewById(R.id.userInfoUsernameTv)
        email = requireView().findViewById(R.id.userInfoEmailTv)
        infoTitle = requireView().findViewById(R.id.userInfoSmallTitleTv)
        infoTv = requireView().findViewById(R.id.userInfoTv)
        ratingTv = requireView().findViewById(R.id.userCurrentRatingTv)
        bigTitleTv = requireView().findViewById(R.id.userInfoTitle)
        ratingBar = requireView().findViewById(R.id.userInfoRatingBar)
        infoTextField = requireView().findViewById(R.id.userInfoTextField)
        infoEditText = requireView().findViewById(R.id.newInfoEt)
        infoScrollView = requireView().findViewById(R.id.infoScrollView)

        backButton = requireView().findViewById(R.id.userInfoBackButton)
        editSaveButton = requireView().findViewById(R.id.editSaveInfoButton)
        submitRatingButton = requireView().findViewById(R.id.userInfoSubmitRatingButton)
        cancelEditButton = requireView().findViewById(R.id.userInfoCancelEditButton)

        // ----- Rescale some stuff if screen is too short --------
        val displayMetrics = resources.displayMetrics
        val displayHeight = displayMetrics.heightPixels / displayMetrics.density
        // val displayWidth = displayMetrics.widthPixels / displayMetrics.density

        if (displayHeight < (activity as MainActivity).minimumHeightDensity) {
            bigTitleTv.textSize = 25f
            username.textSize = 22f
        }
        // ---------------------------------------------------------

        // Check if this profile is the current user's
        var isCurrentUserProfile = false
        if ((activity as MainActivity).isLoggedIn) {
            if (auth.currentUser!!.uid == userID) isCurrentUserProfile = true
        } else {
            submitRatingButton.visibility = View.GONE
        }

        // Don't let user rating himself if it's his profile
        if (isCurrentUserProfile) {

            submitRatingButton.visibility = View.GONE

            ratingBar.setOnTouchListener { v, event -> true }

        } else {

            email.visibility = View.GONE
            editSaveButton.visibility = View.GONE

        }

        // editSave Button let's the user edit his contact info, then save it
        var isEditing = false
        editSaveButton.setOnClickListener {
            if (!isEditing) {

                isEditing = true
                infoScrollView.visibility = View.GONE
                infoTextField.visibility = View.VISIBLE

                // If user hasn't filled his info, it won't pass the predefined text to the text field
                if ((activity as MainActivity).filledInfo) infoEditText.setText(infoTv.text)
                editSaveButton.text = getString(R.string.save)
                cancelEditButton.visibility = View.VISIBLE

            } else {

                val newInfo = infoEditText.text!!.toString().trim()
                if (newInfo.length < 10) {

                    Toast.makeText(context,
                        getString(R.string.refill_fields),
                        Toast.LENGTH_SHORT).show()

                } else {

                    // Set user as "has filled his info", and add that info
                    val newData = hashMapOf(
                        "info" to newInfo,
                        "filledInfo" to true
                    )

                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.caution))
                        .setMessage(getString(R.string.edit_info_warning))
                        .setNeutralButton(getString(R.string.cancel)) { dialog, _ ->
                            // User clicks cancel

                            dialog.dismiss()
                        }
                        .setPositiveButton(getString(R.string.confirm)) { dialog, _ ->

                            dialog.dismiss()

                            (activity as MainActivity).createLoadingDialog()

                            db.runBatch { batch ->
                                batch.set(db.collection("users").document(auth.currentUser!!.uid), newData, SetOptions.merge())
                            }.addOnCompleteListener {

                                (activity as MainActivity).filledInfo = true
                                isEditing = false
                                infoTextField.visibility = View.GONE
                                infoScrollView.visibility = View.VISIBLE
                                infoTv.text = newInfo
                                editSaveButton.text = getString(R.string.edit)
                                cancelEditButton.visibility = View.GONE

                                (activity as MainActivity).dismissLoadingDialog()

                            }
                        }
                        .show()
                }

            }

        }

        // Button cancels the editing of contact info
        cancelEditButton.setOnClickListener {
            isEditing = false
            infoTextField.visibility = View.GONE
            infoScrollView.visibility = View.VISIBLE
            editSaveButton.text = getString(R.string.edit)
            cancelEditButton.visibility = View.GONE
        }

        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Gets the rating from the rating bar then adds it to profile owner's ratings
        // Calculates the new average rating of all available ratings and sets it in the database
        submitRatingButton.setOnClickListener {

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.confirm))
                .setMessage("${getString(R.string.rate_user_warning)} ${ratingBar.rating}/5.0 ${getString(R.string.stars)}?")
                .setNeutralButton(getString(R.string.cancel)) { dialog, _ ->
                    // User clicks cancel

                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.rate)) { dialog, _ ->

                    dialog.dismiss()

                    (activity as MainActivity).createLoadingDialog()

                    // Get all ratings
                    db.collection("users").document(userID).collection("ratings").get(Source.SERVER)
                        .addOnSuccessListener { ratings ->

                            var newRating = 0f

                            db.runBatch { batch ->
                                val currentRatingRef = db.collection("users").document(userID).collection("ratings").document(auth.currentUser!!.uid)

                                // Add this new rating
                                batch.set(currentRatingRef, hashMapOf("rating" to ratingBar.rating))

                                // Calculate new average rating
                                var userHasAlreadyRated = false
                                newRating = 0f

                                // Sums all the ratings, if current user has already rated this profile, his rating won't be included in the sum
                                // Because it will be overwritten
                                for (rating in ratings) {
                                    if (rating.id == auth.currentUser!!.uid) {
                                        userHasAlreadyRated = true
                                    } else {
                                        newRating += rating.data["rating"].toString().toFloat()
                                    }
                                }

                                var someVar = 1
                                if (userHasAlreadyRated) someVar = 0
                                newRating += ratingBar.rating
                                newRating /= (ratings.size() + someVar)

                                // Sets that new average rating
                                batch.set(db.collection("users").document(userID), hashMapOf("rating" to newRating), SetOptions.merge())

                            }.addOnCompleteListener {
                                ratingTv.text = "%.1f".format(newRating).plus("/5.0 ${getString(R.string.stars)}")

                                (activity as MainActivity).dismissLoadingDialog()

                            }.addOnFailureListener {
                                (activity as MainActivity).dismissLoadingDialog()

                                Toast.makeText(
                                    context,
                                    it.localizedMessage,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        }.addOnFailureListener {
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

        (activity as MainActivity).createLoadingDialog()

        // Fetching all user data and setting them in the fields
        source = if ((activity as MainActivity).isOnline()) Source.DEFAULT else Source.CACHE
        db.collection("users").document(userID).get(source)
            .addOnSuccessListener { user ->

                if (!user.exists()) {
                    showNoUserError()
                } else {

                    val localUsername = user.data!!["username"].toString()
                    val filledInfo = user.data!!["filledInfo"].toString().toBoolean()
                    username.text = "${getString(R.string.username)}: ".plus(localUsername)
                    email.text = user.data!!["email"].toString()
                    infoTitle.text = localUsername.plus(getString(R.string.contact_info))

                    // If user has filled his info, display it, else display predefined text
                    if (filledInfo) {
                        infoTv.text = user.data!!["info"].toString()
                    } else {
                        infoTv.text = getString(R.string.no_user_info)
                    }

                    // Setting the rating has 3 different possibilities
                    if (user.data!!["rating"] == null) {
                        // 1: No rating, leave rating bar empty and display this text

                        ratingTv.text = getString(R.string.not_rated)

                        (activity as MainActivity).dismissLoadingDialog()

                    } else {
                        val thisRating = user.data!!["rating"].toString().toFloat()

                        ratingTv.text = "%.1f".format(thisRating).plus("/5.0 ${getString(R.string.stars)}")

                        if (isCurrentUserProfile) {
                            // 2: User has been rated and this is his profile
                            // The rating bar will display the average rating & the user can't change it

                            ratingBar.rating = user.data!!["rating"].toString().toFloat()

                            (activity as MainActivity).dismissLoadingDialog()

                        } else {
                            // 3: User has been rated and this isn't his profile
                            // Get this current user's rating of the profile's owner and display it in the rating bar, user can modify it and submit a new rating
                            // If current user hasn't rated the profile's owner, rating bar will be left empty

                            db.collection("users").document(userID)
                                .collection("ratings").document(auth.currentUser!!.uid).get(source)
                                .addOnSuccessListener { rating ->

                                    if (rating.exists()) {
                                        ratingBar.rating = rating.data!!["rating"]?.toString()!!.toFloat()
                                    }

                                    (activity as MainActivity).dismissLoadingDialog()

                                }.addOnFailureListener {
                                    (activity as MainActivity).dismissLoadingDialog()

                                    Toast.makeText(
                                        context,
                                        it.localizedMessage,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                        }
                    }
                }
            }.addOnFailureListener {
                showNoUserError()
            }
    }

    private fun showNoUserError() {
        (activity as MainActivity).dismissLoadingDialog()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.error))
            .setMessage(getString(R.string.check_net_error))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.go_back)) { dialog, _ ->
                parentFragmentManager.popBackStack()
            }.show()

    }

}