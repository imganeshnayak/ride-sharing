package dz.notacompany.riide

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LoginFragment : Fragment(R.layout.fragment_login) {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var emailEt : TextInputEditText
    private lateinit var passwordEt : TextInputEditText

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth
        db = Firebase.firestore

        val confirmButton = getView()?.findViewById<Button>(R.id.confirmLoginButton)
        val cancelButton = getView()?.findViewById<Button>(R.id.loginCancelButton)
        emailEt = requireView().findViewById(R.id.loginEmailEt)
        passwordEt = requireView().findViewById(R.id.loginPasswordEt)

        cancelButton?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        confirmButton?.setOnClickListener {
            login()
        }
    }

    private fun login() {

        var email = emailEt.text.toString().trim().lowercase()
        val password = passwordEt.text.toString().trim()
        var emailToSave = ""
        var usernameToSave = ""

        if (email.isNotBlank() && password.isNotBlank()) {

            (activity as MainActivity).createLoadingDialog()

            // Check if the input is already taken as a username
            // else just log the user assuming the input is an email
            db.collection("users").whereEqualTo("username",email).get(Source.SERVER)
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        if (document != null) {
                            usernameToSave = email
                            email = document.data["email"].toString()
                        }
                        emailToSave = email
                    }

                    // Authenticate user after checking if the username exists
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(requireActivity()) { task ->
                            if (task.isSuccessful) {

                                // Sign in success, update UI with the signed-in user's information
                                Toast.makeText(context,
                                    getString(R.string.logged_in_success),
                                    Toast.LENGTH_SHORT).show()

                                (activity as MainActivity).userEmail = emailToSave

                                // If the username is unknown, fetch it and store it
                                if (usernameToSave == "") {
                                    db.collection("users").document(auth.currentUser!!.uid).get(Source.SERVER)
                                        .addOnSuccessListener {
                                            (activity as MainActivity).username = it.data!!["username"].toString()

                                            // Refreshes the activity to logged in state and sets the new fragment
                                            (activity as MainActivity).refreshMainActivity()
                                            (activity as MainActivity).navBar.selectedItemId =
                                                R.id.page_profile

                                            (activity as MainActivity).dismissLoadingDialog()
                                        }
                                        .addOnFailureListener {
                                            (activity as MainActivity).dismissLoadingDialog()
                                            Toast.makeText(context,
                                                it.localizedMessage,
                                                Toast.LENGTH_SHORT).show()
                                        }
                                } else {
                                    // Username is known

                                    (activity as MainActivity).username = usernameToSave

                                    // Refreshes the main activity to fetch user data, and set the new fragment
                                    (activity as MainActivity).refreshMainActivity()
                                    (activity as MainActivity).setCurrentFragment(
                                        ProfileLoggedinFragment()
                                    )

                                    (activity as MainActivity).dismissLoadingDialog()
                                }
                            } else {

                                (activity as MainActivity).dismissLoadingDialog()

                                // If sign in fails, display a message to the user.
                                Toast.makeText(context,
                                    task.exception?.localizedMessage.toString(),
                                    Toast.LENGTH_SHORT).show()
                            }
                        }

                }
                .addOnFailureListener {
                    // Error while checking if the username already exists
                    (activity as MainActivity).dismissLoadingDialog()
                    Toast.makeText(
                        context,
                        it.localizedMessage,
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            Toast.makeText(
                context, getString(R.string.refill_fields),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}