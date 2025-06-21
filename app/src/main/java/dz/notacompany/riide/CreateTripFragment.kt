package dz.notacompany.riide

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

class CreateTripFragment : Fragment(R.layout.fragment_adding_trip), DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

    // Current time vars
    private var day = 0
    private var month = 0
    private var year = 0
    private var hour = 0
    private var minute = 0

    // Input time vars
    private var savedDay = 0
    private var savedMonth = 0
    private var savedYear = 0
    private var savedHour = 0
    private var savedMinute = 0

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var departureTv : TextView
    private lateinit var destinationTv : TextView
    private lateinit var dateTimeTv : TextView
    private lateinit var passengerNumTv : TextView
    private lateinit var cancelButton : Button
    private lateinit var chooseTimeButton : Button
    private lateinit var confirmButton : Button
    private lateinit var passengerNumMinusButton : Button
    private lateinit var passengerNumPlusButton : Button
    private lateinit var priceInputEt : TextInputEditText

    private lateinit var departure : String
    private lateinit var destination : String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        departureTv = requireView().findViewById(R.id.departureTv)
        destinationTv = requireView().findViewById(R.id.destinationTv)
        dateTimeTv = requireView().findViewById(R.id.dateTimeTv)
        cancelButton = requireView().findViewById(R.id.cTripCancelButton)
        chooseTimeButton = requireView().findViewById(R.id.chooseTimeButton)
        confirmButton = requireView().findViewById(R.id.cTripConfirmButton)
        priceInputEt = requireView().findViewById(R.id.priceInputEf)
        passengerNumMinusButton = requireView().findViewById(R.id.passengerNumMinusButton)
        passengerNumPlusButton = requireView().findViewById(R.id.passengerNumPlusButton)
        passengerNumTv = requireView().findViewById(R.id.passengerNumTv)

        auth = Firebase.auth
        db = Firebase.firestore

        departure = (activity as MainActivity).departure
        destination = (activity as MainActivity).destination

        departureTv.text = departure
        destinationTv.text = destination


        getDateTimeCalendar()
        // Init the Tv with the current time
        (activity as MainActivity).displayTimeInTv(dateTimeTv,hour,minute,day,month,year)

        chooseTimeButton.setOnClickListener {
            // Show the DatePicker, which will then show the TimePicker
            DatePickerDialog(requireContext(),this,year,month,day).show()
        }

        passengerNumMinusButton.setOnClickListener {
            // Decrement number of passengers
            val number = passengerNumTv.text.toString().toInt()
            if (number > 1) {
                passengerNumTv.text = (number - 1).toString()
            }
        }

        passengerNumPlusButton.setOnClickListener {
            // Increment number of passengers
            val number = passengerNumTv.text.toString().toInt()
            if (number < 9) {
                passengerNumTv.text = (number + 1).toString()
            }
        }

        confirmButton.setOnClickListener {

            // Check if the user is already in a trip
            // User cannot be in several trips at the same time
            if (!(activity as MainActivity).isInTrip) {

                // Check if price input field isn't empty
                if (priceInputEt.text.toString() != "") {

                    // Makes the Trip data to save to the database
                    // Separated into several hashMaps because context
                    val price = priceInputEt.text.toString().toInt()
                    val maxPassengers = passengerNumTv.text.toString().toInt()
                    val tripDate = hashMapOf(
                        "year" to savedYear,
                        "month" to savedMonth,
                        "day" to savedDay,
                        "hour" to savedHour,
                        "minute" to savedMinute,
                    )

                    val driver = hashMapOf(
                        "userID" to auth.currentUser!!.uid,
                        "username" to (activity as MainActivity).username,
                    )

                    val trip = hashMapOf(
                        "driver" to driver,
                        "departure" to (activity as MainActivity).departure,
                        "destination" to (activity as MainActivity).destination,
                        "date" to tripDate,
                        "price" to price,
                        "maxPassengers" to maxPassengers,
                        "seatsLeft" to maxPassengers,
                    )

                    if (!(activity as MainActivity).filledInfo) {
                        // Contact info missing

                        Toast.makeText(context,
                            getString(R.string.fill_contact_info),
                            Toast.LENGTH_SHORT).show()

                    } else {
                        // User has filled in his info

                        (activity as MainActivity).createLoadingDialog()

                        // Add trip hashMap to the database as a document with a random ID
                        // Set isInTrip user field to true
                        // Add the new trip's ID as "currentTripID" in user document
                        db.runBatch { batch ->

                            val userDocument = db.collection("users").document(auth.currentUser!!.uid)

                            batch.set(db.collection("trips").document(auth.currentUser!!.uid), trip)
                            batch.update(userDocument, "isInTrip", true)
                            batch.set(userDocument, hashMapOf("currentTripID" to auth.currentUser!!.uid), SetOptions.merge())

                        }.addOnCompleteListener {
                            (activity as MainActivity).dismissLoadingDialog()
                            (activity as MainActivity).isInTrip = true
                            (activity as MainActivity).currentTripID = auth.currentUser!!.uid
                            parentFragmentManager.popBackStack()

                            Toast.makeText(
                                context,
                                getString(R.string.trip_created),
                                Toast.LENGTH_SHORT
                            ).show()

                        }.addOnFailureListener {
                            (activity as MainActivity).dismissLoadingDialog()

                            Toast.makeText(
                                context,
                                it.localizedMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                } else {
                    // Price input field empty
                    Toast.makeText(context,
                        getString(R.string.give_price),
                        Toast.LENGTH_SHORT).show()
                }
            } else {
                // User is already in a trip
                parentFragmentManager.popBackStack()

                Toast.makeText(context,
                    getString(R.string.cannot_be_in_several_trips),
                    Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

    }

    // Get the current date and time
    private fun getDateTimeCalendar() {
        val cal = Calendar.getInstance()
        day = cal.get(Calendar.DAY_OF_MONTH)
        savedDay = day
        month = cal.get(Calendar.MONTH)
        savedMonth = month
        year = cal.get(Calendar.YEAR)
        savedYear = year
        hour = cal.get(Calendar.HOUR)
        savedHour = hour
        minute = cal.get(Calendar.MINUTE)
        savedMinute = minute
    }

    // Function runs when positive button in DatePicker dialog is clicked
    override fun onDateSet(p0: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        savedDay = dayOfMonth
        // For some damn reason, month starts from index 0, so i'm adding 1
        savedMonth = month+1
        savedYear = year

        // Show the TimePicker after choosing the date
        TimePickerDialog(requireContext(),this,hour,minute,true).show()
    }

    // Function runs when positive button in TimePicker dialog is clicked
    override fun onTimeSet(p0: TimePicker?, hourOfDay: Int, tMinute: Int) {
        savedHour = hourOfDay
        savedMinute = tMinute

        // Checks if the date isn't in the past
        if ((savedYear < year) || (savedDay < day && savedMonth == month+1 && savedYear == year) || (savedMonth < month+1 && savedYear == year) || (savedHour < hour && savedDay == day && savedMonth == month+1 && savedYear == year) || (savedMinute < minute && savedHour == hour && savedDay == day && savedMonth == month+1 && savedYear == year)) {
            Toast.makeText(context,
                getString(R.string.wrong_time_chosen),
                Toast.LENGTH_SHORT).show()
        } else {
            (activity as MainActivity).displayTimeInTv(dateTimeTv,savedHour,savedMinute,savedDay,savedMonth,savedYear)
        }
    }
}