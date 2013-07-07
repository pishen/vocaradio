var errorCount = 0;
var glowing = true;
var chatSocket = null;

$(document).ready(function(){
	$("nav li").on("click", function(){
		$("nav li.selected").removeClass("selected");
		$("#right-panel > div.selected").removeClass("selected");
		$(this).addClass("selected");
		$("#right-panel > div").eq($(this).index()).addClass("selected");
	});
	
	$("audio").on("error", function(e){
		errorCount++;
		if(errorCount < 5){
			this.load();
			this.play();
		}
		window.setTimeout(function(){
			errorCount--;
		}, 10000);
	});
	
	$("audio").on("pause", function(e){
		var source = $("audio > source");
		var streamUrl = source.attr("src");
		source.attr("src", "");
		this.load();
		source.attr("src", streamUrl);
	});
	
	$("span#air").on("click", function(e){
		if(glowing){
			glowing = false;
			$("#air").toggleClass("glow", false);
		}else{
			glowing = true;
			if($("span#air").hasClass("on")){
				$("#air").toggleClass("glow", true);
			}
		}
	});
	
	chatSocket = new WebSocket("ws://dg.pishen.info/vocaradio/chat");
	chatSocket.onmessage = function(message){
		$("div#chat-log").append("<p>" + message.data + "</p>");
	};
	
	$("textarea#new-chat").keydown(function(e){
		if(e.which == 13 && $(this).val() != ""){
			chatSocket.send($(this).val());
			$(this).val("");
			return false;
		}
	});
	
	getStatus();
});

function getStatus(){
	$.getJSON("status", function(data){
		if(data.onAir == "false"){
			$("#air").toggleClass("on", false).toggleClass("glow", false).text("OFF AIR");		
			$("#listen-num").text("0 listener.");
			$("#title").text("--");
		}else{
			$("#air").toggleClass("on", true).text("ON AIR");
			if(glowing){
				$("#air").toggleClass("glow", true);
			}
			
			if(data.num == "0" || data.num == "1"){
				$("#listen-num").text(data.num + " listener.");
			}else{
				$("#listen-num").text(data.num + " listeners.");
			}
			if($("#title").text() != data.title){
				$("#title").text(data.title);
			}
		}
	});
	window.setTimeout(getStatus, 12000);
}
