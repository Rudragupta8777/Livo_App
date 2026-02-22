package com.livo.works.screens.fragments

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.livo.works.databinding.FragmentProfileBinding
import com.livo.works.screens.Login
import com.livo.works.security.TokenManager
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var tokenManager: TokenManager

    // 1. Declare an animator variable to manage the blinking effect safely
    private var dotAnimator: ObjectAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadAndDecodeUserData()
        setupClickListeners()
    }

    private fun loadAndDecodeUserData() {
        val token = tokenManager.getAccessToken()

        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Session Error", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val parts = token.split(".")
            if (parts.size < 2) return

            val payloadBase64 = parts[1]
            val payloadBytes = Base64.decode(payloadBase64, Base64.URL_SAFE)
            val payloadString = String(payloadBytes, Charsets.UTF_8)

            val jsonObject = JSONObject(payloadString)

            val name = jsonObject.optString("name", "User")
            val email = jsonObject.optString("email", "No Email")

            val rolesArray = jsonObject.optJSONArray("roles")
            val roles = mutableListOf<String>()
            if (rolesArray != null) {
                for (i in 0 until rolesArray.length()) {
                    roles.add(rolesArray.getString(i))
                }
            }

            binding.tvUserName.text = name
            binding.tvUserEmail.text = email

            if (name.isNotEmpty()) {
                binding.tvInitials.text = name.substring(0, 1).uppercase()
            }

            val isHotelManager = roles.contains("HOTEL_MANAGER")
            val isLivoInternal = roles.contains("LIVO_INTERNAL")

            when {
                isLivoInternal -> {
                    // Admin Tag: Soft Green Theme
                    binding.statusBar.visibility = View.VISIBLE

                    binding.tvRoleText.text = "ADMIN"
                    binding.tvRoleText.setTextColor(android.graphics.Color.parseColor("#1B5E20")) // Dark Green Text
                    binding.indicatorDot.setCardBackgroundColor(android.graphics.Color.parseColor("#4CAF50")) // Bright Green Dot
                    binding.cvRoleBadge.setCardBackgroundColor(android.graphics.Color.parseColor("#E8F5E9")) // Soft Green BG
                    binding.cvRoleBadge.strokeColor = android.graphics.Color.parseColor("#C8E6C9") // Green Outline

                    // Floating Crown
                    binding.cvSpecialBadge.visibility = View.VISIBLE
                    binding.tvBadgeIcon.text = "👑"

                    startBlinkingDot() // Start the animation
                }
                isHotelManager -> {
                    // Manager Tag: Sleek Monochrome Theme
                    binding.statusBar.visibility = View.VISIBLE

                    binding.tvRoleText.text = "HOTEL MANAGER"
                    binding.tvRoleText.setTextColor(android.graphics.Color.parseColor("#000000")) // Black Text
                    binding.indicatorDot.setCardBackgroundColor(android.graphics.Color.parseColor("#000000")) // Black Dot
                    binding.cvRoleBadge.setCardBackgroundColor(android.graphics.Color.parseColor("#F5F5F5")) // Light Grey BG
                    binding.cvRoleBadge.strokeColor = android.graphics.Color.parseColor("#E0E0E0") // Grey Outline

                    // Floating Star
                    binding.cvSpecialBadge.visibility = View.VISIBLE
                    binding.tvBadgeIcon.text = "🧑‍💼"

                    startBlinkingDot() // Start the animation
                }
                else -> {
                    // Guest Tag
                    binding.statusBar.visibility = View.GONE // Hides the entire status bar

                    binding.tvRoleText.text = "GUEST"
                    binding.tvRoleText.setTextColor(android.graphics.Color.parseColor("#757575")) // Grey Text
                    binding.indicatorDot.setCardBackgroundColor(android.graphics.Color.parseColor("#9E9E9E")) // Grey Dot
                    binding.cvRoleBadge.setCardBackgroundColor(android.graphics.Color.parseColor("#FAFAFA")) // Off-white BG
                    binding.cvRoleBadge.strokeColor = android.graphics.Color.parseColor("#EEEEEE") // Soft Outline

                    binding.cvSpecialBadge.visibility = View.GONE

                    dotAnimator?.cancel() // Stop animation if Guest
                }
            }

            // 4. Handle Visibility of Action Items
            if (isLivoInternal) {
                binding.layoutAdminPanel.visibility = View.VISIBLE
            } else {
                binding.layoutAdminPanel.visibility = View.GONE
            }

            if (isHotelManager) {
                binding.btnListHotel.visibility = View.GONE
                binding.btnManageHotels.visibility = View.VISIBLE
            } else {
                binding.btnListHotel.visibility = View.VISIBLE
                binding.btnManageHotels.visibility = View.GONE
            }

        } catch (e: Exception) {
            e.printStackTrace()
            binding.tvUserName.text = "User"
            binding.tvUserEmail.text = tokenManager.getEmail()
            binding.cvRoleBadge.visibility = View.GONE
            binding.cvSpecialBadge.visibility = View.GONE
        }
    }

    // 2. Add the animation method
    private fun startBlinkingDot() {
        // Cancel any existing animation first to prevent stacking
        dotAnimator?.cancel()

        // Animate the alpha property from 1f (solid) to 0.3f (faded)
        dotAnimator = ObjectAnimator.ofFloat(binding.indicatorDot, View.ALPHA, 1f, 0.3f).apply {
            duration = 800 // 800ms per fade
            repeatMode = ValueAnimator.REVERSE // Fade out, then fade back in
            repeatCount = ValueAnimator.INFINITE // Do it forever
            start()
        }
    }

    private fun setupClickListeners() {
        binding.btnListHotel.setOnClickListener {
            Toast.makeText(requireContext(), "Opening Access Request...", Toast.LENGTH_SHORT).show()
        }

        binding.btnAdminPanel.setOnClickListener {
            Toast.makeText(requireContext(), "Opening Admin Panel...", Toast.LENGTH_SHORT).show()
        }

        binding.btnManageHotels.setOnClickListener {
            Toast.makeText(requireContext(), "Opening Properties...", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun performLogout() {
        tokenManager.clear()
        val intent = Intent(requireContext(), Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 3. Prevent memory leaks by canceling the animation when the view is destroyed
        dotAnimator?.cancel()
        dotAnimator = null
        _binding = null
    }
}