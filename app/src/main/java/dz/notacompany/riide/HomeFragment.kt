package dz.notacompany.riide

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import android.widget.ToggleButton
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var passengerDriverToggle : ToggleButton
    private lateinit var searchCreateButton : Button
    private lateinit var allTripsButton : Button
    private lateinit var departure : TextInputEditText
    private lateinit var destination : TextInputEditText

    private var isPassenger = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        passengerDriverToggle = requireView().findViewById(R.id.driverPassengerToggle)
        searchCreateButton = requireView().findViewById(R.id.searchCreateButton)
        allTripsButton = requireView().findViewById(R.id.allTripsButton)
        departure = requireView().findViewById(R.id.departureEt)
        destination = requireView().findViewById(R.id.destinationEt)

        // Checks for the passenger/driver toggle state
        passengerDriverToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                searchCreateButton.text = getString(R.string.create)
                isPassenger = false
            } else {
                searchCreateButton.text = getString(R.string.search)
                isPassenger = true
            }
        }

        allTripsButton.setOnClickListener {
            (activity as MainActivity).replaceCurrentFragment(DisplayTripsFragment(1))
        }

        searchCreateButton.setOnClickListener {

            // Put departure and destination from user input to variables in the activity
            (activity as MainActivity).departure = departure.text.toString().trim().lowercase()
            (activity as MainActivity).destination = destination.text.toString().trim().lowercase()

            if ((activity as MainActivity).departure == "" || (activity as MainActivity).destination == "") {
                // Empty field

                Toast.makeText(
                    context, getString(R.string.refill_fields),
                    Toast.LENGTH_SHORT
                ).show()

            } else {
                // Departure and destination input should be valid

                if (isPassenger) {
                    // Search for a trip

                    departure.setText("")
                    destination.setText("")

                    (activity as MainActivity).replaceCurrentFragment(DisplayTripsFragment(0))

                } else {
                    // Create a trip

                    // Checks if the user is logged in
                    if ((activity as MainActivity).isLoggedIn) {

                        departure.setText("")
                        destination.setText("")

                        (activity as MainActivity).replaceCurrentFragment(CreateTripFragment())
                    } else {

                        // User not logged in
                        Toast.makeText(
                            context, getString(R.string.not_logged_in_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            }
        }
    }
}