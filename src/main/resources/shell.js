var template = `
<style>
    ::-webkit-scrollbar {
        width: 15px;
    }
    ::-webkit-scrollbar-track {
        border-left: 1px solid var(--border-color) !important;
        border-right: 1px solid var(--border-color) !important;
        background-color: white;
    }
    ::-webkit-scrollbar-thumb {
        background-color: #c1c1c1;
    }
    :root {
        --blue: #097bed;
        --orange: #ff6c37;
        --light-grey: #f9f9f9;
        --dark-grey: #f2f2f2;
        --border-color: #ededed;
    }
    #exec-button {
        height: 40px;
        border: 0px;
        width: 80px;
        min-width: 80px;
        font-weight: 800;
        background-color: var(--blue);
        text-align: center;
        border-radius: 5px !important;
        line-height: 40px;
        color: white;
        cursor: pointer;
    }
    .container-list {
        border: 1px solid var(--border-color) !important;
        border-radius: 5px;
        margin-bottom: 10px;
    }
    .container-item {
        text-align: left;
        color: black;
        font-weight: 500;
        background-color: var(--light-grey);
        height: 30px;
        line-height: 30px;
        padding-left: 14px;
        margin: 0px;
        cursor: pointer;
    }
    .running > .container-item::before {
        content: '● ';
        font-size: 20px;
        color: green;
    }
    .stopped > .container-item::before {
        content: '● ';
        font-size: 20px;
        color: red;
       
    }
    .stopped {
        pointer-events: none;
        cursor: default !important;
    }
    .container-item ~ .container-item::before {
        content: '';
    }
    .container-item + .container-item {
        border-top: 2px solid var(--dark-grey) !important;
    } 
    .activated > label {
        box-shadow: inset 0px 3px 0px 0px var(--orange);
    }
    .activated > label + label {
        box-shadow: inset 0px 0px 0px 0px var(--orange);
    }
    #containerList {
        padding-right: 10px;
        padding-top: 10px;
        width: 300px;
        flex-direction: column;
        overflow: hidden;
        overflow-y: scroll;
    }
    input:read-only {
        background-color: var(--dark-grey) !important;
        user-select: none;
        font-weight: 100 !important;
        color: #bbbbbb;
        font-style: italic;
        cursor: default;
    }
    input {
        font-size: 14px;
        font-family: "DejaVu Sans Mono";
        height: 40px !important;
        border: 1px solid var(--border-color) !important;
        border-radius: 4px;
        background-color: var(--light-grey) !important;
        margin-left: 10px;
        margin-right: 10px;
    }
    #process {
        min-width: 100px;
    }
    #inputfield {
        width: 100%;
    }
    #tool-window {
        height: 50px;
        padding-top: 10px;
        line-height: 50px;
    }
    #content {
        font-family: "DejaVu Sans Mono";
        font-weight: 400;
        font-size: 14px;
        width: 100%;
        display: flex;
        flex-direction: column-reverse;
        overflow-y: scroll;
        padding-left: 10px; 
        margin-right: 10px;
        margin-bottom: 0px;
        border-bottom: 1px solid var(--dark-grey) !important;
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
    @media (max-width: 1300px) {
        #content-window {
            flex-direction: column-reverse !important;
            width: 98vw;    
            height: 99vh;
            margin-right: 0px !important;
            padding-right: 10px;
            border: 0px !important;
        }
        #containerList {
            height: 50%;
            width: 100%;
            padding: 0px;
        }
        .container-list {
            margin-right: 10px;
        }
        .container-list {
        }
        #content {
            margin-right: 0px;
            border-top: 1px solid #ededed !important;
        }
    }
</style>

<div id="content-window">
    <div id="console-window" style="display:flex;flex-direction:column; height: 100%; width:100%;">
        <div style="display:flex; flex-grow: 1; overflow: hidden;" >
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
        xhr.open('POST', '{{url}}/container/'+containerId.id+'/write', false);
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

function focusProcessField(input){
    var processfield = document.getElementById("process")
    document.getElementById("process").removeAttribute("readonly");
    document.getElementById("exec-button").classList.remove("disabled");
    processField.value = input;
    setProcessFieldSize(processField);
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
    initSession.open('POST', '{{url}}/container/'+containerId.id+'/session', true);
    initSession.seenBytes = 0;
    initSession.setRequestHeader('Content-type', 'application/json');

    focusInputField();

    var label = document.getElementById("content");
    label.innerHTML = 'connected to process</br>';
    initSession.onreadystatechange = function() {
        if(initSession.readyState == 3) {
            var newData = initSession.response.substr(initSession.seenBytes);
            var splitted = newData.split("\\n");
            for(var i = 0; i < splitted.length - 1; i++){
                if(splitted[i] !== ""){
                    label.innerHTML += splitted[i] + "<br />";
                }
            }
            if('' !== splitted[splitted.length -1]){
                processField.value = splitted[splitted.length -1];
            }
            setProcessFieldSize(processField);
            initSession.seenBytes = initSession.responseText.length;
        }
    };

    initSession.addEventListener("error", function(e) {
        containerId.session = undefined;
        focusProcessField('');
    });

    initSession.addEventListener("load", function(e) {
        containerId.session = undefined;
        focusProcessField('');
    });
    
    initSession.send(command);
}

function setProcessFieldSize(processField){
    processField.style.width = ((processField.value.length + 2)) + "ch";
}

function loadContainerList(){
    var containerListRequest = new XMLHttpRequest();
    containerListRequest.open('GET', '{{url}}/container/list', true);

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
                    
                    focusProcessField('');
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
    focusProcessField('bash');
    loadContainerList();
    setProcessFieldSize(processField);
}

</script>
`;

pm.visualizer.set(template, {
    url: pm.variables.get('shell-url')
});