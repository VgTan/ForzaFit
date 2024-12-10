package com.example.forzafit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserAdapter(private val usernames: Array<String>, private val onAddFriendClick: (String) -> Unit) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val username = usernames[position]
        holder.usernameTextView.text = username

        // Set click listener for add friend button
        holder.addFriendButton.setOnClickListener {
            onAddFriendClick(username)  // Pass the username to the callback when the button is clicked
        }
    }

    override fun getItemCount(): Int {
        return usernames.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val usernameTextView: TextView = itemView.findViewById(R.id.username)
        val addFriendButton: ImageButton = itemView.findViewById(R.id.add_friend_button)
    }
}
