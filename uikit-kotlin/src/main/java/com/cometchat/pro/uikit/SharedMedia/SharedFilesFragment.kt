package com.cometchat.pro.uikit.SharedMedia

import adapter.SharedMediaAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.pro.constants.CometChatConstants
import com.cometchat.pro.core.CometChat.CallbackListener
import com.cometchat.pro.core.MessagesRequest
import com.cometchat.pro.core.MessagesRequest.MessagesRequestBuilder
import com.cometchat.pro.exceptions.CometChatException
import com.cometchat.pro.models.BaseMessage
import com.cometchat.pro.models.MediaMessage
import com.cometchat.pro.uikit.R
import com.google.android.material.snackbar.Snackbar
import listeners.ClickListener
import listeners.RecyclerTouchListener
import utils.MediaUtils
import java.util.*

/**
 * Purpose - It is a Fragment which is used in SharedMedia to display Files sent or recieved to a
 * particular reciever.
 *
 * Created On - 20th March 2020
 *
 * Modified On - 23rd March 2020
 *
 */
class SharedFilesFragment : Fragment() {
    private var rvFiles: RecyclerView? = null
    private var adapter: SharedMediaAdapter? = null
    private var Id: String? = null
    private var type: String? = null
    private var messagesRequest: MessagesRequest? = null
    private val messageList: MutableList<BaseMessage> = ArrayList()
    private var noMedia: LinearLayout? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.shared_media_view, container, false)
        rvFiles = view.findViewById(R.id.rvFiles)
        noMedia = view.findViewById(R.id.no_media_available)
        rvFiles!!.setLayoutManager(GridLayoutManager(context, 2))
        Id = arguments!!.getString("Id")
        type = arguments!!.getString("type")
        fetchMessage()
        rvFiles!!.addOnItemTouchListener(RecyclerTouchListener(context, rvFiles!!, object : ClickListener() {
            override fun onClick(var1: View, var2: Int) {
                val message = var1.getTag(R.string.baseMessage) as BaseMessage
                MediaUtils.openFile((message as MediaMessage).attachment.fileUrl, context!!)
            }
        }))
        rvFiles!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (!recyclerView.canScrollVertically(1)) {
                    fetchMessage()
                }
            }
        })
        return view
    }

    /**
     * This method is used to fetch a messages whose type is MESSAGE_TYPE_FILE.
     * @see MessagesRequest.fetchNext
     */
    private fun fetchMessage() {
        if (messagesRequest == null) {
            messagesRequest = if (type != null && type == CometChatConstants.RECEIVER_TYPE_USER) MessagesRequestBuilder().setCategories(Arrays.asList(CometChatConstants.CATEGORY_MESSAGE)).setTypes(Arrays.asList(CometChatConstants.MESSAGE_TYPE_FILE)).setUID(Id!!).setLimit(30).build() else MessagesRequestBuilder().setCategories(Arrays.asList(CometChatConstants.CATEGORY_MESSAGE)).setTypes(Arrays.asList(CometChatConstants.MESSAGE_TYPE_FILE)).setGUID(Id!!).setLimit(30).build()
        }
        messagesRequest!!.fetchPrevious(object : CallbackListener<List<BaseMessage>>() {
            override fun onSuccess(baseMessageList: List<BaseMessage>) {
                if (baseMessageList.size != 0) setAdapter(baseMessageList)
                checkMediaVisble()
            }

            override fun onError(e: CometChatException) {
                if (rvFiles != null) Snackbar.make(rvFiles!!, "Failed to load Files", Snackbar.LENGTH_LONG).show()
            }
        })
    }

    /**
     * This method is used to check if the size of messages fetched is greater than 0 or not. If it
     * is 0 than it will show "No media Available" message.
     */
    private fun checkMediaVisble() {
        if (messageList.size != 0) {
            rvFiles!!.visibility = View.VISIBLE
            noMedia!!.visibility = View.GONE
        } else {
            noMedia!!.visibility = View.VISIBLE
            rvFiles!!.visibility = View.GONE
        }
    }

    /**
     * This method is used to setAdapter for File messages.
     * @param baseMessageList is object of List<BaseMessage> which contains list of messages.
     * @see SharedMediaAdapter
    </BaseMessage> */
    private fun setAdapter(baseMessageList: List<BaseMessage>) {
        val filteredList = removeDeletedMessage(baseMessageList)
        messageList.addAll(filteredList)
        if (adapter == null) {
            adapter = SharedMediaAdapter(context!!, filteredList)
            rvFiles!!.adapter = adapter
        } else adapter!!.updateMessageList(filteredList)
    }

    /**
     * This method is used to remove deleted messages from message list and return filteredlist.
     * (baseMessage whose deletedAt !=0 must be removed from message list)
     *
     * @param baseMessageList is object of List<BaseMessage>
     * @return filteredMessageList which does not have deleted messages.
     * @see BaseMessage
    </BaseMessage> */
    private fun removeDeletedMessage(baseMessageList: List<BaseMessage>): List<BaseMessage> {
        val resultList: MutableList<BaseMessage> = ArrayList()
        for (baseMessage in baseMessageList) {
            if (baseMessage.deletedAt == 0L) {
                resultList.add(baseMessage)
            }
        }
        return resultList
    }
}