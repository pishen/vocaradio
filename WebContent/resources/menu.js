function bodyLoad(){
	var lis = document.querySelectorAll("nav li");
	for(var i = 0; i < lis.length; i++){
		lis[i].onclick = menuItemClicked;
	}
}

function menuItemClicked(){
	document.querySelector("nav li.selected").setAttribute("class", null);
	document.querySelector("div#right-panel>div.selected").setAttribute("class", null);
	this.setAttribute("class", "selected");
	var lis = document.querySelectorAll("nav li");
	for(var i = 0; i < lis.length; i++){
		if(lis[i].className == "selected"){
			document.querySelectorAll("div#right-panel>div")[i].setAttribute("class", "selected");
			break;
		}
	}
}
