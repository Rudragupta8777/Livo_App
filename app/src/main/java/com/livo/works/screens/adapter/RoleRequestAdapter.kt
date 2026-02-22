package com.livo.works.screens.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.livo.works.Role.data.RoleRequestDto
import com.livo.works.databinding.ItemRoleRequestBinding

class RoleRequestAdapter(
    private val onApprove: (Long) -> Unit,
    private val onReject: (Long) -> Unit
) : ListAdapter<RoleRequestDto, RoleRequestAdapter.RequestViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val binding = ItemRoleRequestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RequestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RequestViewHolder(private val binding: ItemRoleRequestBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(request: RoleRequestDto) {
            binding.apply {
                tvUserName.text = request.userName
                tvUserEmail.text = request.userEmail
                tvRequestedRole.text = request.requestedRole.replace("_", " ")

                // Set Initial
                if (request.userName.isNotEmpty()) {
                    tvRequestInitials.text = request.userName.take(1).uppercase()
                }

                btnApprove.setOnClickListener { onApprove(request.id) }
                btnReject.setOnClickListener { onReject(request.id) }
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<RoleRequestDto>() {
        override fun areItemsTheSame(oldItem: RoleRequestDto, newItem: RoleRequestDto): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RoleRequestDto, newItem: RoleRequestDto): Boolean {
            return oldItem == newItem
        }
    }
}