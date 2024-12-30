package com.example.forzafit

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class UserAdapter(
    private var usernames: Array<String>,
    private var imageUrls: Array<String?>,
    private val onAddFriendClick: (String, String?) -> Unit // Tambahkan parameter URL gambar
) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {

    fun updateData(newUsernames: Array<String>, newImageUrls: Array<String?>) {
        usernames = newUsernames
        imageUrls = newImageUrls
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val username = usernames[position]
        val imageUrl = imageUrls[position]

        holder.usernameTextView.text = username

        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(R.drawable.profile)
            .error(R.drawable.profile)
            .into(holder.profileImageView)

        holder.addFriendButton.setOnClickListener {
            onAddFriendClick(username, imageUrl) // Panggil callback dengan data username dan URL gambar
        }
    }

    override fun getItemCount(): Int {
        return usernames.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val usernameTextView: TextView = itemView.findViewById(R.id.username)
        val profileImageView: ImageView = itemView.findViewById(R.id.profile_image)
        val addFriendButton: ImageButton = itemView.findViewById(R.id.add_friend_button)
    }
}
