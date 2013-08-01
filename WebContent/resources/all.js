var errorCount = 0;
var glowing = true;
var chatSocket = null;

$(document).ready(function(){
	//tab selection handler
	$("nav li").on("click", function(){
		$("nav li.selected").removeClass("selected");
		$("#right-panel > div.selected").removeClass("selected");
		$(this).addClass("selected");
		$("#right-panel > div").eq($(this).index()).addClass("selected");
	});
	
	handleStream();
	handleGlowingSwitch();
	handleWebSocket();
	
	$("textarea#new-chat").keydown(function(e){
		if(e.which == 13 && $(this).val() != ""){
			chatSocket.send($(this).val());
			$(this).val("");
			return false;
		}
	});
	
	getUserInfo(); //along with Persona setting
	getStatus(); //self-invoking function
});

function handleStream(){
	//handle the music stopping problem of Chrome
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
	
	//clean the cache when music is paused
	$("audio").on("pause", function(e){
		var source = $("audio > source");
		var streamUrl = source.attr("src");
		source.attr("src", "");
		this.load();
		source.attr("src", streamUrl);
	});
}

function handleGlowingSwitch(){
	$("span#air").on("click", function(e){
		if(glowing){
			glowing = false;
			$(this).toggleClass("glow", false);
		}else{
			glowing = true;
			if($("span#air").hasClass("on")){
				$(this).toggleClass("glow", true);
			}
		}
	});
}

function handleWebSocket(){
	//WebSocket only for receiving
	chatSocket = new WebSocket("ws://dg.pishen.info:8080/vocaradio/s/ws");
	chatSocket.onmessage = function(evt){
		var json = JSON.parse(evt.data);
		$("div#chat-log").append("<p>" + json.msg + "</p>");
	};
}

function getUserInfo(){
	$.getJSON("s/user-info", function(userInfoJSON){
		var userEmail = userInfoJSON.email;
		
		//Persona watcher
		navigator.id.watch({
			loggedInUser: userEmail,
			onlogin: function(assertion){
				$.post("s/login", assertion).done(function(data){
					window.location.reload();
				}).fail(function(){
					navigator.id.logout();
				});
			},
			onlogout: function(){
				$.get("s/logout", function(data){
					window.location.reload();
				});
			}
		});
		
		//settle the info to display
		if(userEmail == null){
			$("div#auth > span").text("Sign in");
			$("div#auth").on("click", function(e){
				navigator.id.request();
			});
			
			$("div#user-info").hide();
		}else{
			$("div#auth > span").text("Log out");
			$("div#auth").on("click", function(e){
				navigator.id.logout();
			});
			
			$("div#user-info > li#user-email").text("email: " + userEmail);
		}
		
		//display the info
		$("div#user").show();
	});
}

function getStatus(){
	$.getJSON("s/status", function(statusJSON){
		if(statusJSON.onAir == false){
			$("#air").toggleClass("on", false).toggleClass("glow", false).text("OFF AIR");		
			$("#listen-num").text("0 listener.");
			$("#title").text("--");
		}else{
			$("#air").toggleClass("on", true).text("ON AIR");
			if(glowing){
				$("#air").toggleClass("glow", true);
			}
			
			if(statusJSON.num == "0" || statusJSON.num == "1"){
				$("#listen-num").text(statusJSON.num + "/20 listener.");
			}else{
				$("#listen-num").text(statusJSON.num + "/20 listeners.");
			}
			if($("#title").text() != statusJSON.title){
				$("#title").text(statusJSON.title);
			}
		}
	});
	window.setTimeout(getStatus, 12000);
}
