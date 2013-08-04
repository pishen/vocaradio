var errorCount = 0;
var glowing = true;
var chatStickToButtom = true;
var chatSocket = null;

$(document).ready(function(){
	//tab selection handler
	$("nav li").on("click", function(){
		$("nav li.selected").removeClass("selected");
		$("#right-panel > div").toggleClass("hidden", true);
		$(this).addClass("selected");
		$("#right-panel > div").eq($(this).index()).removeClass("hidden");
	});
	
	$("div#chat-log").mouseup(function(){
		if($(this).scrollTop() + $(this).innerHeight() == $(this)[0].scrollHeight){
			chatStickToButtom = true;
		}else{
			chatStickToButtom = false;
		}
	});
	
	$("textarea#new-chat").keydown(function(e){
		if(e.which == 13 && $(this).val() != ""){
			$.post("s/new-chat", $(this).val());
			$(this).val("");
			return false;
		}
	});
	
	handleAudioStream();
	handleGlowingSwitch();
	handleWebSocket();
	handleAuth(); //update userInfo and Persona setting
	
	getStatus(); //self-invoking function
});

function handleAudioStream(){
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
		if(json.type == "chat"){
			$("div#chat-log").append("<p>" + json.name + ": " + json.content + "</p>");
			if(chatStickToButtom){
				$("div#chat-log").animate({
					scrollTop: $("div#chat-log")[0].scrollHeight - $("div#chat-log").innerHeight()
				});
			}
		}
	};
}

function handleAuth(){
	$.getJSON("s/user-info", function(userInfo){
		console.log("email:"+ userInfo.email);
		
		//Persona watcher
		navigator.id.watch({
			loggedInUser: userInfo.email,
			onlogin: function(assertion){
				console.log("login");
				$.post("s/login", assertion).done(function(newUserInfo){
					console.log("login success");
					updateUserInfo(JSON.parse(newUserInfo));
				}).fail(function(){
					console.log("login fail");
					navigator.id.logout();
				});
			},
			onlogout: function(){
				console.log("logout");
				$.get("s/logout");
				updateUserInfo({});
			}
		});
		
		updateUserInfo(userInfo);
	});
}

function updateUserInfo(userInfo){
	//settle the info to display
	$("div#auth").off();
	if(userInfo.email == null){
		$("div#auth > span").text("Sign in with Persona");
		$("div#auth").on("click", function(e){
			navigator.id.request();
		});
		
		//disable chat box
		$("textarea#new-chat").attr("placeholder", "Sign in to chat").prop("disabled", true);
		
		$("div#user-info").toggleClass("hidden", true);
	}else{
		$("div#auth > span").text("Log out");
		$("div#auth").on("click", function(e){
			navigator.id.logout();
		});
		
		$("li#user-email").text("email: " + userInfo.email);
		
		//open the chat sending handler
		$("textarea#new-chat").attr("placeholder", "Your message...").prop("disabled", false);
		
		$("div#user-info").toggleClass("hidden", false);
	}
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
