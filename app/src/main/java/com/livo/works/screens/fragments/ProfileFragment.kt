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
import androidx.lifecycle.lifecycleScope
import com.livo.works.Auth.repository.AuthRepository
import com.livo.works.Role.repository.RoleRepository
import com.livo.works.databinding.FragmentProfileBinding
import com.livo.works.screens.AdminRequests
import com.livo.works.screens.Login
import com.livo.works.screens.ManagerProperties
import com.livo.works.security.TokenManager
import com.livo.works.util.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var tokenManager: TokenManager

    @Inject
    lateinit var roleRepository: RoleRepository
    @Inject
    lateinit var authRepository: AuthRepository

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
        if (token.isNullOrEmpty()) return

        try {
            val parts = token.split(".")
            val payloadString = String(Base64.decode(parts[1], Base64.URL_SAFE), Charsets.UTF_8)
            val jsonObject = JSONObject(payloadString)

            val name = jsonObject.optString("name", "User")
            val email = jsonObject.optString("email", "No Email")
            val rolesArray = jsonObject.optJSONArray("roles")
            val roles = mutableListOf<String>()
            rolesArray?.let { for (i in 0 until it.length()) roles.add(it.getString(i)) }

            binding.tvUserName.text = name
            binding.tvUserEmail.text = email
            if (name.isNotEmpty()) binding.tvInitials.text = name.substring(0, 1).uppercase()

            val isHotelManager = roles.contains("HOTEL_MANAGER")
            val isLivoInternal = roles.contains("LIVO_INTERNAL")

            when {
                isLivoInternal -> {
                    binding.statusBar.visibility = View.VISIBLE
                    binding.tvRoleText.text = "ADMIN"
                    binding.tvRoleText.setTextColor(android.graphics.Color.parseColor("#1B5E20"))
                    binding.indicatorDot.setCardBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
                    binding.cvRoleBadge.setCardBackgroundColor(android.graphics.Color.parseColor("#E8F5E9"))
                    binding.cvRoleBadge.strokeColor = android.graphics.Color.parseColor("#C8E6C9")
                    binding.cvSpecialBadge.visibility = View.VISIBLE
                    binding.tvBadgeIcon.text = "👑"
                    startBlinkingDot()
                }
                isHotelManager -> {
                    binding.statusBar.visibility = View.VISIBLE
                    binding.tvRoleText.text = "HOTEL MANAGER"
                    binding.tvRoleText.setTextColor(android.graphics.Color.parseColor("#1B5E20"))
                    binding.indicatorDot.setCardBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
                    binding.cvRoleBadge.setCardBackgroundColor(android.graphics.Color.parseColor("#E8F5E9"))
                    binding.cvRoleBadge.strokeColor = android.graphics.Color.parseColor("#C8E6C9")
                    binding.cvSpecialBadge.visibility = View.VISIBLE
                    binding.tvBadgeIcon.text = "🧑‍💼"
                    startBlinkingDot()
                }
                else -> {
                    binding.statusBar.visibility = View.GONE
                    binding.cvSpecialBadge.visibility = View.GONE
                    dotAnimator?.cancel()
                }
            }

            binding.layoutAdminPanel.visibility = if (isLivoInternal) View.VISIBLE else View.GONE
            if (isHotelManager) {
                binding.btnListHotel.visibility = View.GONE
                binding.btnManageHotels.visibility = View.VISIBLE
            } else {
                binding.btnListHotel.visibility = View.VISIBLE
                binding.btnManageHotels.visibility = View.GONE
            }

        } catch (e: Exception) {
            binding.cvRoleBadge.visibility = View.GONE
        }
    }

    private fun startBlinkingDot() {
        dotAnimator?.cancel()
        dotAnimator = ObjectAnimator.ofFloat(binding.indicatorDot, View.ALPHA, 1f, 0.3f).apply {
            duration = 800
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }
    }

    private fun setupClickListeners() {
        binding.btnListHotel.setOnClickListener {
            requestManagerAccess()
        }

        binding.btnAdminPanel.setOnClickListener {
            startActivity(Intent(requireContext(), AdminRequests::class.java))
        }

        // ONLY THIS WAS CHANGED: Navigate to ManagerPropertiesActivity instead of a Toast
        binding.btnManageHotels.setOnClickListener {
            startActivity(Intent(requireContext(), ManagerProperties::class.java))
        }

        binding.btnLogout.setOnClickListener {
            handleLogout()
        }
    }

    private fun requestManagerAccess() {
        viewLifecycleOwner.lifecycleScope.launch {
            roleRepository.requestHotelManager().collect { state ->
                when (state) {
                    is UiState.Loading -> binding.btnListHotel.isEnabled = false
                    is UiState.Success -> {
                        binding.btnListHotel.isEnabled = true
                        Toast.makeText(requireContext(), "Access Request Submitted!", Toast.LENGTH_LONG).show()
                    }
                    is UiState.Error -> {
                        binding.btnListHotel.isEnabled = true
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                    is UiState.SessionExpired -> navigateToLogin()
                    else -> {}
                }
            }
        }
    }
    private fun handleLogout() {
        viewLifecycleOwner.lifecycleScope.launch {
            authRepository.logoutUser().collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.btnLogout.isEnabled = false
                        binding.btnLogout.text = "Logging out..."
                    }
                    is UiState.Success -> {
                        navigateToLogin()
                    }
                    is UiState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        navigateToLogin()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(requireContext(), Login::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dotAnimator?.cancel()
        dotAnimator = null
        _binding = null
    }
}