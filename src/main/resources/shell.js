var template = `
<style>
    #exec-button {
        height: 40px;
        border: 0px solid #dddddd;
        border-top: 1px solid #ededed;
        width: 80px;
        min-width:80px;
        background-color: #93d5e3;
        text-align: center;
        border-radius: 0px !important;
        line-height: 40px;
        color: black;
        padding-top: 0px;
        cursor: pointer;
    }
    .container-list {
        border: 1px solid #dddddd !important;
        margin-bottom: 10px;
    }
    .container-item {
        text-align: left;
        background-color: #dddddd;
        height: 30px;
        line-height: 30px;
        padding-left: 14px;
        margin: 0px;
        cursor: pointer !important;
    }
    .container-list:hover > .container-item {
        padding-left: 11px !important;
    }
    .container-item + .container-item {
        background-color: white;
    } 
    .activated {
        border-left: 4px solid orange !important;
    }
    .activated > .container-item {
        padding-left: 11px !important;
    }
    #containerList {
        border-left: 1px solid #dddddd !important;
        padding-left: 10px;
        padding-right: 10px;
        padding-top: 10px;
        width: 300px;
        flex-direction: column;
        overflow: hidden;
        overflow-y: scroll;
    }
    .container-list:hover {
        border-top: 1px solid orange !important;
        border-bottom: 1px solid orange !important;
        border-left: 4px solid orange !important;
        cursor: pointer !important;
    }
    input {
        font-size: 15px;
        font-family: "DejaVu Sans Mono";
        height: 40px !important;
        border: 0px solid black !important;
        border-radius: 0px !important;
        border-top: 1px solid #ededed !important;
    }
    #process {
        min-width: 100px;
    }
    #inputfield {
        width: 100%;
    }
    #tool-window {
        height: 40px;
    }
    #process:read-only, #inputfield:read-only {
        background-color: #dddddd;
        user-select: none;
        color: grey;
        cursor: default;
    }
    #process:read-only {
        border-right: 1px solid #ededed !important;
    }
    #content {
        font-family: "DejaVu Sans Mono";
        font-weight: 500;
        width: 100%;
        display: flex;
        flex-direction: column-reverse;
        overflow-y: scroll;
        padding-left: 10px; 
    }
    .running {
        border-right: 10px solid green !important;
    }
    .stopped {
        border-right: 10px solid red !important;
    }
    .disabled {
        pointer-events: none;
        background-color: #dddddd !important;
        color: grey !important;
        cursor: default !important;
    }
    #content-window {
        display:flex; 
        overflow: hidden; 
        flex-direction: row; 
        width:99%; 
        height:99%;
        border: 1px solid #ededed !important;
    }
</style>

<div id="content-window">
    <div style="display:flex;flex-direction:column; width:100%; height:100%;">
        <div style="display:flex; flex-grow: 1; overflow: hidden; ">
            <label id="content"></label>
        </div>
        <div id="tool-window" style="display:flex;">
            <input type="text" id="process" value="bash">
            <a id="exec-button">Exec</a>
            <input type="text" id="inputfield">
        </div>
    </div>
    <div id="containerList" style="display:flex;">
    </div>
</div>

<script>

var commandList = new Array();
var commandPointer = new Object();
commandPointer.index = -1;
var containerId = new Object();

const inputField = document.getElementById("inputfield");
inputField.addEventListener("keyup", ({key}) => {
    if (key === "Enter") {
        var xhr = new XMLHttpRequest();
        xhr.open('POST', 'http://localhost:8080/container/'+containerId.id+'/write', false);
        var toBeSend = inputField.value.replaceAll('"','\\\\"');
        commandList.push(inputField.value);
        inputField.value = '';
        xhr.send(toBeSend);
    }
})
inputField.addEventListener("keydown", ({key}) => {
    if (key === "ArrowUp") { 
        commandPointer.index++;
        if(commandPointer.index >= commandList.length){
            commandPointer.index--;
            return;
        }
        var index = commandList.length - 1 - commandPointer.index;
        inputField.value = commandList[index];
    } else if (key === "ArrowDown"){
        commandPointer.index--;
        if(commandPointer.index < 0){
            inputField.value = '';
            commandPointer.index++;
            return;
        }
        var index = commandList.length - 1 - commandPointer.index;
        inputField.value = commandList[index];
    }
});

const processField = document.getElementById("process");
processField.addEventListener("keyup", ({key}) => {
    if (key === "Enter") {
        onExecButton();
    }
});

document.getElementById("exec-button").onclick = () => {
    onExecButton();
};

function onExecButton(){
    if(containerId.id){
        createSession(processField.value);
        inputfield.value = '';  
    } else {
        alert("please select container first");
    }
}

processField.addEventListener('input', function (input) {
    setProcessFieldSize(processField);
});

function focusProcessField(){
    var processfield = document.getElementById("process")
    document.getElementById("process").removeAttribute("readonly");
    document.getElementById("exec-button").classList.remove("disabled");
    processfield.focus();
    processfield.select();
    var inputfield = document.getElementById("inputfield");
    inputfield.setAttribute("readonly", "true");
    inputfieldPlaceholder();
}

function inputfieldPlaceholder(){
    if(containerId.id){
        inputfield.value = "Enter process"
    } else {
        inputfield.value = "Select container"
    }
}

function focusInputField(){
    var inputField = document.getElementById("inputfield")
    inputField.removeAttribute("readonly");
    inputField.focus();
    inputField.select();
    document.getElementById("process").setAttribute("readonly", "true");
    document.getElementById("exec-button").classList.add("disabled");
}

function createSession(command){
    var initSession = new XMLHttpRequest();

    containerId.session = initSession;
    initSession.open('POST', 'http://localhost:8080/container/'+containerId.id+'/session', true);
    initSession.seenBytes = 0;
    initSession.setRequestHeader('Content-type', 'application/json');

    focusInputField();

    var label = document.getElementById("content");
    label.innerHTML = 'connected to process</br>';
    initSession.onreadystatechange = function() {
        var newData = initSession.response.substr(initSession.seenBytes);
        var splitted = newData.split("\\n");
        for(var i = 0; i < splitted.length - 1; i++){
            if(splitted[i] !== ""){
                label.innerHTML += splitted[i] + "<br />";
            }
        }
        processField.value = splitted[splitted.length -1];
        setProcessFieldSize(processField);
        initSession.seenBytes = initSession.responseText.length;
    };

    initSession.addEventListener("error", function(e) {
        containerId.session = undefined;
        focusProcessField();
    });

    initSession.addEventListener("load", function(e) {
        containerId.session = undefined;
        focusProcessField();
    });
    
    initSession.send(command);
}

function setProcessFieldSize(processField){
    processField.style.width = ((processField.value.length + 2)) + "ch";
}

function loadContainerList(){
    var containerListRequest = new XMLHttpRequest();
    containerListRequest.open('GET', 'http://localhost:8080/container/list', true);

    containerListRequest.onload = function() {
        var json = JSON.parse(containerListRequest.response);
        var parentDiv = document.getElementById("containerList");
        parentDiv.innerHTML = '';
        for(var i = 0; i < json.length; i++){
            var obj = new Object();
            obj.id = json[i].Id.substring(0, 16);

            var div = document.createElement("div");
            var nameLabel = document.createElement("Label");
            var idLabel = document.createElement("Label");
            nameLabel.innerHTML = json[i].Image;
            idLabel.innerHTML = obj.id;
            div.classList.add("container-list");

            if(json[i].State === "exited"){
                div.classList.add('stopped');
                console.log("happened!");
            }

            if(json[i].State === "running"){
                div.classList.add('running');
                console.log("happened running!");
            }

            nameLabel.classList.add("container-item");
            idLabel.classList.add("container-item");
            
            div.onclick = (e) => {
                var arr = document.getElementsByClassName("activated");
                if(arr.length > 0){
                    if(containerId.session){
                        if(!confirm("you have an open session, are you sure you want to cancel it?")){
                            return;
                        } else {
                            containerId.session.abort();
                            containerId.session = undefined;
                        }
                    }
                    
                    focusProcessField();
                    var label = document.getElementById("content");
                    label.innerHTML = '';

                    oldDiv = arr[0];
                    oldDiv.classList.remove("activated");
                }

                var newDiv = document.getElementById(e.srcElement.parentElement.id);
                containerId.id = e.srcElement.parentElement.id;
                newDiv.classList.add("activated");

                inputfieldPlaceholder();
            };
            div.id = obj.id;
            parentDiv.appendChild(div);
            div.appendChild(nameLabel);
            div.appendChild(idLabel);
        }
    }

    containerListRequest.send();
}

document.onload = new function () {
    focusProcessField();
    loadContainerList();
    setProcessFieldSize(processField);
}

</script>
`;


pm.visualizer.set(template, {

});