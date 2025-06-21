package dz.notacompany.riide

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val signupButton = getView()?.findViewById<Button>(R.id.signupButton)
        val loginButton = getView()?.findViewById<Button>(R.id.loginButton)

        // These two basically call the method to switch fragment from MainActivity
        signupButton?.setOnClickListener {
            (activity as MainActivity).replaceCurrentFragment(SignupFragment())
        }
        loginButton?.setOnClickListener {
            (activity as MainActivity).replaceCurrentFragment(LoginFragment())
        }
    }
}