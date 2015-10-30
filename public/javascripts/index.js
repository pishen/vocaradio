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

//chat

//playlist
function updatePlaylist() {
    $.getJSON("/playlist", function(songs) {
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
    })
}

//websocket
var socket
var secondsToWait = 1

setInterval(function() {
    socket.send("")
}, 300000)

function updateSocket() {
    socket = new WebSocket("ws://" + window.location.host + "/socket")
    socket.onopen = function() {
        console.log("websocket connected.")
        secondsToWait = 1
        socket.send("")
            //TODO update the information at connection lost
        updatePlaylist()
    }
    socket.onmessage = function(event) {
        var json = JSON.parse(event.data)
        if (json.msg == "updatePlaylist") {
            updatePlaylist()
        } else if (json.msg == "") {
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
}

var toSync = true
var isFirst = true

function onPlayerStateChange(event) {
    if (event.data == YT.PlayerState.ENDED) {
        //if come from opening video, seek, otherwise just continue next song
        play()
    } else if (event.data == YT.PlayerState.PLAYING) {
        if (toSync && !isFirst) {
            play()
        }
        isFirst = false
        $("#playback").text("PAUSE").off().click(function() {
            player.pauseVideo()
        })
    } else if (event.data == YT.PlayerState.PAUSED) {
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
        console.log("playing id: " + json.song.id)
        player.loadVideoById(json.song.id, toSync ? json.playedSeconds : 0)
        toSync = false
        player.setVolume($("#volume").prop("value"))
    })
}
