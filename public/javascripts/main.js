$(document).ready(function() {
	var tag = document.createElement('script');
	tag.src = "https://www.youtube.com/iframe_api";
	var firstScriptTag = document.getElementsByTagName('script')[0];
	firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);
	
	$("#sync").on("click", function(){
		$.getJSON("sync", function(jsObj){
			player.loadVideoById(jsObj.id);
		});
	});
});

var player;
function onYouTubeIframeAPIReady() {
	
	$.getJSON("sync", function(jsObj){
		player = new YT.Player('player', {
			height : '360',
			width : '640',
			videoId : jsObj.id,
			playerVars : {
				'autoplay' : 1,
				'start' : 0
			},
			events : {
				'onReady' : onPlayerReady,
				'onStateChange' : onPlayerStateChange
			}
		});
	});
}

function onPlayerReady(event) {
}

function onPlayerStateChange(event) {
	if(event.data == YT.PlayerState.ENDED){
		$.getJSON("sync", function(jsObj){
			player.loadVideoById(jsObj.id);
		});
	}
}