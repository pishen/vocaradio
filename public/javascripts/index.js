//notification
function notify(theTitle, theBody) {
    new Notification(theTitle, {
        body: theBody,
        icon: "/assets/images/logo.png"
    })
}

Notification.requestPermission(function() {
    notify("VocaRadio", "訊息通知已啟動")
})

//online-list

//username
var username = $("#username")

if (localStorage.username) {
    username.val(localStorage.username)
}

username.blur(function() {
    localStorage.username = username.val()
    socket.send(username.val())
})

//chat
$("#new-chat").keydown(function(e) {
    if (e.keyCode == 13) {
        $.post("/chat", {
            name: username.val(),
            text: $(this).val()
        }, function() {
            $("#new-chat").val("")
        }).fail(function() {
            $(".settings-btn").click()
        })
        return false
    }
})

//playlist
function updatePlaylist(songs) {
    var songIds = songs.map(function(song) {
        return song.id
    })

    $(".song").filter(function(i, s) {
        return songIds.indexOf(s.id) < 0
    }).remove()

    songs.forEach(function(song, i) {
        var target = $("#" + song.id.replace(/(#|:|\.|\[|\]|,)/g, "\\$1"))
        if (target.length != 0) {
            target.attr("class", "song pos" + i).html(song.html)
        } else {
            $("<div></div>").attr("id", song.id).addClass("song pos" + i).html(song.html).appendTo("#playlist")
        }
    })

    $(".song button.request").off().click(function() {
        $.post("/request", {
            id: $(this).closest(".song").attr("id"),
            name: username.val()
        }).fail(function() {
            $(".settings-btn").click()
        })
    })
}

//websocket
var socket
var secondsToWait = 1

setInterval(function() {
    socket.send(username.val())
}, 300000)

function updateSocket() {
    socket = new WebSocket("wss://" + window.location.host + "/socket")
    socket.onopen = function() {
        console.log("websocket connected.")
        secondsToWait = 1
        socket.send(username.val())
            //TODO update the information at connection lost, maybe we don't need this now?
    }
    socket.onmessage = function(event) {
        //TODO bind notifications
        var json = JSON.parse(event.data)
        if (json.msgType == "updatePlaylist") {
            updatePlaylist(json.msg)
        } else if (json.msgType == "play") {
            toSync = true
            play()
        } else if (json.msgType == "updateStatus") {
            $("#numOfListeners").text(json.msg.numOfListeners)
                //TODO update clientNames
            $("#clientNames").text(json.msg.clientNames.join(", "))
        } else if (json.msgType == "appendChat") {
            $("#chat-log").append(json.msg.html)
            $("#chat-log").scrollTop($("#chat-log").prop("scrollHeight"))
            if (json.msg.user != username.val()) {
                notify(json.msg.user, json.msg.text)
            }
        } else if (json.msgType == "reloadChat") {
            $("#chat-log").html(json.msg.html)
            $("#chat-log").scrollTop($("#chat-log").prop("scrollHeight"))
        } else if (json.msgType == "") {
            //TODO
        }
    }
    socket.onclose = function() {
        console.log("websocket closed, retry in " + secondsToWait + " seconds.")
        setTimeout(updateSocket, secondsToWait * 1000)
        secondsToWait *= 2
    }
}
updateSocket()

//youtube player
var tag = document.createElement('script')
tag.src = "https://www.youtube.com/iframe_api"
var firstScriptTag = document.getElementsByTagName('script')[0]
firstScriptTag.parentNode.insertBefore(tag, firstScriptTag)

var player = null

function onYouTubeIframeAPIReady() {
    player = new YT.Player('player', {
        height: '360',
        width: '640',
        videoId: '7NptssoOJ78',
        playerVars: {
            'wmode': 'opaque',
            'rel': 0,
            'iv_load_policy': 3,
            'controls': 0,
            'disablekb': 1
        },
        events: {
            'onReady': onPlayerReady,
            'onStateChange': onPlayerStateChange,
            'onError': onPlayerError
        }
    })
}

var toSync = false

function onPlayerReady(event) {
    console.log("ready")
    if (localStorage.volume) {
        $("#volume").prop("value", localStorage.volume)
    }
    player.setVolume($("#volume").prop("value"))
    $("#volume").change(function() {
        player.setVolume($(this).prop("value"))
        localStorage.volume = $(this).prop("value")
    })
    $("#playback").click(function() {
        player.playVideo()
    })
    toSync = false
}

function onPlayerStateChange(event) {
    if (event.data == YT.PlayerState.ENDED) {
        //if come from opening video, seek, otherwise just continue next song
        console.log("ended")
        if (player.getVideoData().video_id == "7NptssoOJ78") {
            toSync = true
        }
        play()
    } else if (event.data == YT.PlayerState.PLAYING) {
        console.log("playing")
        if (toSync) {
            play()
        }
        $("#playback").text("PAUSE").off().click(function() {
            player.pauseVideo()
        })
    } else if (event.data == YT.PlayerState.PAUSED) {
        console.log("paused")
        toSync = true
        $("#playback").text("PLAY").off().click(function() {
            play()
        })
    }
}

function onPlayerError(event) {
    console.log("player error: " + event.data)
    toSync = true
    $("#playback").text("PLAY").off().click(function() {
        play()
    })
}

function play() {
    $.getJSON("/playing", function(json) {
        console.log("playing id: " + json.song.id + ", toSync: " + toSync)
        player.loadVideoById(json.song.id, toSync ? json.playedSeconds : 0)
        toSync = false
        player.setVolume($("#volume").prop("value"))
    })
}
