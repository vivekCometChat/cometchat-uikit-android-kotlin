package utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.core.CometChat.CallbackListener
import com.cometchat.pro.exceptions.CometChatException
import com.cometchat.pro.models.BaseMessage
import com.cometchat.pro.uikit.reaction.model.Reaction
import com.cometchat.pro.uikit.sticker.model.Sticker
import listeners.ExtensionResponseListener
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap

public class Extensions {

    companion object{
        private const val TAG = "Extensions"

        fun extensionCheck(baseMessage: BaseMessage): HashMap<String, JSONObject>? {
            val metadata = baseMessage.metadata
            val extensionMap = HashMap<String, JSONObject>()
            try {
                return if (metadata != null) {
                    val injectedObject = metadata.getJSONObject("@injected")
                    if (injectedObject != null && injectedObject.has("extensions")) {
                        val extensionsObject = injectedObject.getJSONObject("extensions")
                        if (extensionsObject != null && extensionsObject.has("link-preview")) {
                            val linkPreviewObject = extensionsObject.getJSONObject("link-preview")
                            val linkPreview = linkPreviewObject.getJSONArray("links")
                            if (linkPreview.length() > 0) {
                                extensionMap["linkPreview"] = linkPreview.getJSONObject(0)
                            }
                        }
                        if (extensionsObject != null && extensionsObject.has("smart-reply")) {
                            extensionMap["smartReply"] = extensionsObject.getJSONObject("smart-reply")
                        }
                        if (extensionsObject != null && extensionsObject.has("thumbnail-generation")){
                            extensionMap["thumbnailGeneration"] = extensionsObject.getJSONObject("thumbnail-generation")
                        }
                        if (extensionsObject != null && extensionsObject.has("reactions")){
                            extensionMap["reactions"] = extensionsObject.getJSONObject("reactions")
                        }
                    }
                    extensionMap
                } else null
            } catch (e: java.lang.Exception) {
                Log.e(TAG, "isLinkPreview: " + e.message)
            }
            return null
        }

        private fun getSmartReplyList(baseMessage: BaseMessage): List<String>? {
            val extensionList: HashMap<String, JSONObject> = extensionCheck(baseMessage)!!
            if (extensionList != null && extensionList.containsKey("smartReply")) {
                val replyObject = extensionList["smartReply"]
                val replyList: MutableList<String> = ArrayList()
                try {
                    replyList.add(replyObject!!.getString("reply_positive"))
                    replyList.add(replyObject.getString("reply_neutral"))
                    replyList.add(replyObject.getString("reply_negative"))
                } catch (e: java.lang.Exception) {
                    Log.e(TAG, "onSuccess: " + e.message)
                }
                return replyList
            }
            return null
        }

        fun checkSmartReply(lastMessage: BaseMessage?): List<String?>? {
            if (lastMessage != null && lastMessage.sender.uid != CometChat.getLoggedInUser().uid) {
                if (lastMessage.metadata != null) {
                    return getSmartReplyList(lastMessage)
                }
            }
            return null
        }

        fun fetchStickers(extensionResponseListener: ExtensionResponseListener<Any>) {
            CometChat.callExtension("stickers", "GET", "/v1/fetch", null, object : CallbackListener<JSONObject>() {
                override fun onSuccess(jsonObject: JSONObject?) {
                    extensionResponseListener.onResponseSuccess(jsonObject)
                }

                override fun onError(e: CometChatException?) {
                    extensionResponseListener.onResponseFailed(e)
                }

            })
        }

        fun extractStickersFromJSON(jsonObject: JSONObject): HashMap<String?, MutableList<Sticker>?> {
            val stickers: MutableList<Sticker> = ArrayList()
            if (jsonObject != null) {
                try {
                    val dataObject: JSONObject = jsonObject.getJSONObject("data")
                    val defaultStickersArray = dataObject.getJSONArray("defaultStickers")
                    Log.d(TAG, "getStickersList: defaultStickersArray " + defaultStickersArray.length())
                    for (i in 0 until defaultStickersArray.length()) {
                        val stickerObject = defaultStickersArray.getJSONObject(i)
                        val stickerOrder = stickerObject.getString("stickerOrder")
                        val stickerSetId = stickerObject.getString("stickerSetId")
                        val stickerUrl = stickerObject.getString("stickerUrl")
                        val stickerSetName = stickerObject.getString("stickerSetName")
                        val stickerName = stickerObject.getString("stickerName")
                        val sticker = Sticker(stickerName, stickerUrl, stickerSetName)
                        stickers.add(sticker)
                    }
                    if (dataObject.has("customStickers")) {
                        val customSticker = dataObject.getJSONArray("customStickers")
                        Log.d(TAG, "getStickersList: customStickersArray $customSticker")
                        for (i in 0 until customSticker.length()) {
                            val stickerObject = customSticker.getJSONObject(i)
                            val stickerOrder = stickerObject.getString("stickerOrder")
                            val stickerSetId = stickerObject.getString("stickerSetId")
                            val stickerUrl = stickerObject.getString("stickerUrl")
                            val stickerSetName = stickerObject.getString("stickerSetName")
                            val stickerName = stickerObject.getString("stickerName")
                            val sticker = Sticker(stickerName, stickerUrl, stickerSetName)
                            stickers.add(sticker)
                        }
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
            val stickerMap: HashMap<String?, MutableList<Sticker>?> = HashMap()
            for (i in stickers.indices) {
                if (stickerMap.containsKey(stickers[i].setName)) {
                    stickerMap[stickers[i].setName]!!.add(stickers[i])
                } else {
                    val list: MutableList<Sticker> = ArrayList()
                    list.add(stickers[i])
                    stickerMap[stickers[i].setName] = list
                }
            }
            return stickerMap
        }

        fun getThumbnailGeneration(context: Context, baseMessage: BaseMessage): String? {
            var urlSmall:String? = null
            try {
                val extensionList = extensionCheck(baseMessage)
                if (extensionList != null && extensionList.containsKey("thumbnailGeneration")){
                    val thumbnailGenerationObject = extensionList["thumbnailGeneration"]
                    urlSmall = thumbnailGenerationObject!!.getString("url_small")
                }
            }
            catch (e: Exception)
            {
                Toast.makeText(context, "Error:" + e.message, Toast.LENGTH_LONG).show()
            }
            return urlSmall
        }

        fun getInitialReactions(i: Int): MutableList<Reaction> {
            var resultReaction: MutableList<Reaction> = ArrayList()
            var feelReactionList = ReactionUtils.getFeelList()
            for (j in 0..i){
                var reaction:Reaction = feelReactionList[j]
                resultReaction.add(reaction)
            }
            return resultReaction
        }

        fun getReactionsOnMessage(baseMessage: BaseMessage): HashMap<String, String> {
            val resultReactions: HashMap<String, String> = HashMap()
            try {
                var extensionList = extensionCheck(baseMessage)
                if (extensionList != null && extensionList.containsKey("reactions")) {
                    val reactionObject = extensionList["reactions"]
                    var keys: Iterator<String> = reactionObject!!.keys()
                    while (keys.hasNext()){
                        var keyValue :String = keys.next()
                        var reaction : JSONObject = reactionObject.getJSONObject(keyValue)
                        var reactionCount: String = reaction.length().toString()
                        resultReactions[keyValue] = reactionCount
                        Log.e(TAG, "getReactionsOnMessage: $keyValue=$reactionCount")
                    }
                }
            }
            catch (e: Exception){
                e.printStackTrace()
            }
            return resultReactions
        }

        fun getReactionsInfo(jsonObject: JSONObject): HashMap<String, List<String>> {
            val result = HashMap<String, List<String>>()
            if (jsonObject != null) {
                try {
                    val injectedObject = jsonObject.getJSONObject("@injected")
                    if (injectedObject != null && injectedObject.has("extensions")) {
                        val extensionsObject = injectedObject.getJSONObject("extensions")
                        if (extensionsObject.has("reactions")) {
                            val data = extensionsObject.getJSONObject("reactions")
                            val keys = data.keys()
                            while (keys.hasNext()) {
                                val reactionUser: MutableList<String> = ArrayList()
                                val keyValue = keys.next() as String
                                val react = data.getJSONObject(keyValue)
                                val uids = react.keys()
                                while (uids.hasNext()) {
                                    val uid = uids.next() as String
                                    val user = react.getJSONObject(uid)
                                    reactionUser.add(user.getString("name"))
                                    Log.e(TAG, "getReactionsOnMessage: " + keyValue + "=" + user.getString("name"))
                                }
                                result[keyValue] = reactionUser
                            }
                        }
                    }
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
            return result
        }


    }


}