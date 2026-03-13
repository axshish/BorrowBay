package com.example.borrowbay

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.borrowbay.navigation.NavGraph
import com.example.borrowbay.ui.theme.BorrowBayTheme
import com.razorpay.PaymentResultListener

class MainActivity : ComponentActivity(), PaymentResultListener {
    
    // Global bridge for Razorpay callbacks
    companion object {
        var onPaymentSuccess: ((String) -> Unit)? = null
        var onPaymentError: ((Int, String) -> Unit)? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BorrowBayTheme {
                NavGraph()
            }
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        Log.d("MainActivity", "Payment Success: $razorpayPaymentId")
        onPaymentSuccess?.invoke(razorpayPaymentId ?: "")
        // Clear listeners after use to prevent memory leaks or duplicate calls
        onPaymentSuccess = null
        onPaymentError = null
    }

    override fun onPaymentError(code: Int, response: String?) {
        Log.e("MainActivity", "Payment Error ($code): $response")
        onPaymentError?.invoke(code, response ?: "Unknown Error")
        onPaymentSuccess = null
        onPaymentError = null
    }
}
