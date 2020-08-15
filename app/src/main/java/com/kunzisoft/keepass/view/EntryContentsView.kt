/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.kunzisoft.keepass.view

import android.content.Context
import android.graphics.Color
import android.text.util.Linkify
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.adapters.EntryAttachmentsAdapter
import com.kunzisoft.keepass.adapters.EntryHistoryAdapter
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.database.search.UuidUtil
import com.kunzisoft.keepass.model.EntryAttachment
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.otp.OtpType
import java.util.*


class EntryContentsView @JvmOverloads constructor(context: Context,
                                                  var attrs: AttributeSet? = null,
                                                  var defStyle: Int = 0)
    : LinearLayout(context, attrs, defStyle) {

    private var fontInVisibility: Boolean = false

    private val userNameContainerView: View
    private val userNameView: TextView
    private val userNameActionView: ImageView

    private val passwordContainerView: View
    private val passwordView: TextView
    private val passwordActionView: ImageView

    private val otpContainerView: View
    private val otpLabelView: TextView
    private val otpView: TextView
    private val otpActionView: ImageView

    private var otpRunnable: Runnable? = null

    private val urlContainerView: View
    private val urlView: TextView

    private val notesContainerView: View
    private val notesView: TextView

    private val entryExtraFieldsContainerParent: View
    private val entryExtraFieldsContainer: ViewGroup

    private val creationDateView: TextView
    private val modificationDateView: TextView
    private val lastAccessDateView: TextView
    private val expiresImageView: ImageView
    private val expiresDateView: TextView

    private val attachmentsContainerView: View
    private val attachmentsListView: RecyclerView
    private val attachmentsAdapter = EntryAttachmentsAdapter(context, false)

    private val historyContainerView: View
    private val historyListView: RecyclerView
    private val historyAdapter = EntryHistoryAdapter(context)

    private val uuidView: TextView
    private val uuidReferenceView: TextView

    val isUserNamePresent: Boolean
        get() = userNameContainerView.visibility == View.VISIBLE

    val isPasswordPresent: Boolean
        get() = passwordContainerView.visibility == View.VISIBLE

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.view_entry_contents, this)

        userNameContainerView = findViewById(R.id.entry_user_name_container)
        userNameView = findViewById(R.id.entry_user_name)
        userNameActionView = findViewById(R.id.entry_user_name_action_image)

        passwordContainerView = findViewById(R.id.entry_password_container)
        passwordView = findViewById(R.id.entry_password)
        passwordActionView = findViewById(R.id.entry_password_action_image)

        otpContainerView = findViewById(R.id.entry_otp_container)
        otpLabelView = findViewById(R.id.entry_otp_label)
        otpView = findViewById(R.id.entry_otp)
        otpActionView = findViewById(R.id.entry_otp_action_image)

        urlContainerView = findViewById(R.id.entry_url_container)
        urlView = findViewById(R.id.entry_url)

        notesContainerView = findViewById(R.id.entry_notes_container)
        notesView = findViewById(R.id.entry_notes)

        entryExtraFieldsContainerParent = findViewById(R.id.extra_fields_container_parent)
        entryExtraFieldsContainer = findViewById(R.id.extra_fields_container)

        attachmentsContainerView = findViewById(R.id.entry_attachments_container)
        attachmentsListView = findViewById(R.id.entry_attachments_list)
        attachmentsListView?.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, true)
            adapter = attachmentsAdapter
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        }

        creationDateView = findViewById(R.id.entry_created)
        modificationDateView = findViewById(R.id.entry_modified)
        lastAccessDateView = findViewById(R.id.entry_accessed)
        expiresImageView = findViewById(R.id.entry_expires_image)
        expiresDateView = findViewById(R.id.entry_expires_date)

        historyContainerView = findViewById(R.id.entry_history_container)
        historyListView = findViewById(R.id.entry_history_list)
        historyListView?.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, true)
            adapter = historyAdapter
        }

        uuidView = findViewById(R.id.entry_UUID)
        uuidReferenceView = findViewById(R.id.entry_UUID_reference)
    }

    fun applyFontVisibilityToFields(fontInVisibility: Boolean) {
        this.fontInVisibility = fontInVisibility
    }

    fun assignUserName(userName: String?) {
        if (userName != null && userName.isNotEmpty()) {
            userNameContainerView.visibility = View.VISIBLE
            userNameView.apply {
                text = userName
                if (fontInVisibility)
                    applyFontVisibility()
            }
        } else {
            userNameContainerView.visibility = View.GONE
        }
    }

    fun assignUserNameCopyListener(onClickListener: OnClickListener) {
        userNameActionView.setOnClickListener(onClickListener)
    }

    fun assignPassword(password: String?, allowCopyPassword: Boolean) {
        if (password != null && password.isNotEmpty()) {
            passwordContainerView.visibility = View.VISIBLE
            passwordView.apply {
                text = password
                if (fontInVisibility)
                    applyFontVisibility()
            }
            passwordActionView.isActivated = !allowCopyPassword
        } else {
            passwordContainerView.visibility = View.GONE
        }
    }

    fun assignPasswordCopyListener(onClickListener: OnClickListener?) {
        passwordActionView.apply {
            setOnClickListener(onClickListener)
            if (onClickListener == null) {
                visibility = View.GONE
            }
        }
    }

    fun atLeastOneFieldProtectedPresent(): Boolean {
        entryExtraFieldsContainer.let {
            for (i in 0 until it.childCount) {
                val childCustomView = it.getChildAt(i)
                if (childCustomView is EntryExtraField)
                    if (childCustomView.isProtected)
                        return true
            }
        }
        return false
    }

    fun setHiddenPasswordStyle(hiddenStyle: Boolean) {
        passwordView.applyHiddenStyle(hiddenStyle)
        // Hidden style for custom fields
        entryExtraFieldsContainer.let {
            for (i in 0 until it.childCount) {
                val childCustomView = it.getChildAt(i)
                if (childCustomView is EntryExtraField)
                    childCustomView.setHiddenPasswordStyle(hiddenStyle)
            }
        }
    }

    fun assignOtp(otpElement: OtpElement?,
                  otpProgressView: ProgressBar?,
                  onClickListener: OnClickListener) {
        otpContainerView.removeCallbacks(otpRunnable)

        if (otpElement != null) {
            otpContainerView.visibility = View.VISIBLE

            if (otpElement.token.isEmpty()) {
                otpView.text = context.getString(R.string.error_invalid_OTP)
                otpActionView.setColorFilter(ContextCompat.getColor(context, R.color.grey_dark))
                assignOtpCopyListener(null)
            } else {
                assignOtpCopyListener(onClickListener)
                otpView.text = otpElement.token
                otpLabelView.text = otpElement.type.name

                when (otpElement.type) {
                    // Only add token if HOTP
                    OtpType.HOTP -> {
                        otpProgressView?.visibility = View.GONE
                    }
                    // Refresh view if TOTP
                    OtpType.TOTP -> {
                        otpProgressView?.apply {
                            max = otpElement.period
                            progress = otpElement.secondsRemaining
                            visibility = View.VISIBLE
                        }
                        otpRunnable = Runnable {
                            if (otpElement.shouldRefreshToken()) {
                                otpView.text = otpElement.token
                            }
                            otpProgressView?.progress = otpElement.secondsRemaining
                            otpContainerView.postDelayed(otpRunnable, 1000)
                        }
                        otpContainerView.post(otpRunnable)
                    }
                }
            }
        } else {
            otpContainerView.visibility = View.GONE
            otpProgressView?.visibility = View.GONE
        }
    }

    fun assignOtpCopyListener(onClickListener: OnClickListener?) {
        otpActionView.setOnClickListener(onClickListener)
    }

    fun assignURL(url: String?) {
        if (url != null && url.isNotEmpty()) {
            urlContainerView.visibility = View.VISIBLE
            urlView.text = url
        } else {
            urlContainerView.visibility = View.GONE
        }
    }

    fun assignComment(comment: String?) {
        if (comment != null && comment.isNotEmpty()) {
            notesContainerView.visibility = View.VISIBLE
            notesView.apply {
                text = comment
                if (fontInVisibility)
                    applyFontVisibility()
            }
            try {
                Linkify.addLinks(notesView, Linkify.ALL)
            } catch (e: Exception) {}
        } else {
            notesContainerView.visibility = View.GONE
        }
    }

    fun assignCreationDate(date: DateInstant) {
        creationDateView.text = date.getDateTimeString(resources)
    }

    fun assignModificationDate(date: DateInstant) {
        modificationDateView.text = date.getDateTimeString(resources)
    }

    fun assignLastAccessDate(date: DateInstant) {
        lastAccessDateView.text = date.getDateTimeString(resources)
    }

    fun setExpires(isExpires: Boolean) {
        expiresImageView.visibility = if (isExpires) View.VISIBLE else View.GONE
    }

    fun assignExpiresDate(date: DateInstant) {
        assignExpiresDate(date.getDateTimeString(resources))
    }

    fun assignExpiresDate(constString: String) {
        expiresDateView.text = constString
    }

    fun assignUUID(uuid: UUID) {
        uuidView.text = uuid.toString()
        uuidReferenceView.text = UuidUtil.toHexString(uuid)
    }

    /* -------------
     * Extra Fields
     * -------------
     */

    private fun showOrHideExtraFieldsContainer(hide: Boolean) {
        entryExtraFieldsContainerParent.visibility = if (hide) View.GONE else View.VISIBLE
    }

    fun addExtraField(title: String,
                      value: ProtectedString,
                      allowCopy: Boolean,
                      onActionClickListener: OnClickListener?) {

        val entryCustomField: EntryExtraField? = EntryExtraField(context, attrs, defStyle)
        entryCustomField?.apply {
            setLabel(title)
            setValue(value.toString(), value.isProtected)
            activateActionButton(allowCopy)
            assignActionButtonClickListener(onActionClickListener)
            applyFontVisibility(fontInVisibility)
        }
        entryCustomField?.let {
            entryExtraFieldsContainer.addView(it)
        }
        showOrHideExtraFieldsContainer(false)
    }

    fun clearExtraFields() {
        entryExtraFieldsContainer.removeAllViews()
        showOrHideExtraFieldsContainer(true)
    }

    /* -------------
     * Attachments
     * -------------
     */

    private fun showAttachments(show: Boolean) {
        attachmentsContainerView.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun assignAttachments(attachments: ArrayList<EntryAttachment>,
                          onAttachmentClicked: (attachment: EntryAttachment)->Unit) {
        showAttachments(attachments.isNotEmpty())
        attachmentsAdapter.assignAttachments(attachments)
        attachmentsAdapter.onItemClickListener = { item ->
            onAttachmentClicked.invoke(item)
        }
    }

    fun updateAttachmentDownloadProgress(attachmentToDownload: EntryAttachment) {
        attachmentsAdapter.updateProgress(attachmentToDownload)
    }

    /* -------------
     * History
     * -------------
     */

    fun showHistory(show: Boolean) {
        historyContainerView.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun refreshHistory() {
        historyAdapter.notifyDataSetChanged()
    }

    fun assignHistory(history: ArrayList<Entry>) {
        historyAdapter.clear()
        historyAdapter.entryHistoryList.addAll(history)
    }

    fun onHistoryClick(action: (historyItem: Entry, position: Int)->Unit) {
        historyAdapter.onItemClickListener = { item, position ->
                action.invoke(item, position)
            }
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }
}
