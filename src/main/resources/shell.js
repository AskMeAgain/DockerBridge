var template = `
<style>
    input {
        height: 100%;
        box-sizing: border-box;
        border: 0px solid black !important;
        border-top: 1px solid #dddddd !important;
        border-radius: 0px !important;
    }
    input:focus {
    }
    #inputfield {
        width: 100%;
    }
    #inputfield:read-only {
        color: grey;
        background-color: #eeeeee;
        user-select: none;
    }
    #process {
        width: 70px;
        text-align: center;
    }
    #tool-window {
        height: 40px;
        background-color: grey;
    }
    #process:read-only {
        color: grey;
        background-color: #eeeeee;
        user-select: none;
    }
    #content {
        width: 100%;
        display: flex;
        flex-direction: column-reverse;
        overflow-y: scroll;
        padding-left: 10px; 
    }
</style>

<div style="display:flex;flex-direction:column; height:100%;">
    <div style="display:flex; flex-grow: 1; overflow: hidden; ">
        <label id="content"></label>
    </div>
    <div id="tool-window" style="display:flex;">
        <input type="text" id="process" value="bash">
        <input type="text" id="inputfield">
    </div>
</div>

<script>

const inputField = document.getElementById("inputfield");
inputField.addEventListener("keyup", ({key}) => {
    if (key === "Enter") {
        var xhr = new XMLHttpRequest();
        xhr.open('POST', 'http://localhost:8080/container/f5133fb5199f/write', false);

        var toBeSend = inputField.value.replaceAll('"','\\\\"');

        xhr.send(toBeSend);

        inputField.value = '';
    }
})

const processField = document.getElementById("process");
processField.addEventListener("keyup", ({key}) => {
    if (key === "Enter") {
        createSession(processField.value);
    }
})

function focusProcessField(){
    var processfield = document.getElementById("process")
    document.getElementById("process").removeAttribute("readonly");
    processfield.focus();
    processfield.select();
    document.getElementById("inputfield").setAttribute("readonly", "true");
}

function focusInputField(){
    var inputField = document.getElementById("inputfield")
    inputField.removeAttribute("readonly");
    inputField.focus();
    inputField.select();
    document.getElementById("process").setAttribute("readonly", "true");
}

function createSession(command){
    var initSession = new XMLHttpRequest();
    initSession.open('POST', 'http://localhost:8080/container/f5133fb5199f/session', true);
    initSession.seenBytes = 0;
    initSession.setRequestHeader('Content-type', 'application/json');

    focusInputField();

    initSession.onreadystatechange = function() {
        var newData = initSession.response.substr(initSession.seenBytes);
        var label = document.getElementById("content");
        label.innerHTML += newData.replaceAll("\\n", "<br />");
        initSession.seenBytes = initSession.responseText.length;
    };

    initSession.addEventListener("error", function(e) {
        focusProcessField();
    });

    initSession.addEventListener("load", function(e) {
        focusProcessField();
    });
    
    initSession.send(command);
}

document.onload = createSession("bash");

</script>
`;


pm.visualizer.set(template, {

});