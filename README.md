# DockerBridge

This is the ultimate answer to "what should not be done with [Postman](https://www.postman.com/)?"

This bridge allows us to enter docker commands via rest. 
This itself doesnt sounds already good, but in combination
with Postman, we can do some insane stuff.

This project uses [docker-java](https://github.com/docker-java/docker-java)
and hosts a restapi via [spring.io](https://spring.io/)

This repo has an [example collection](/DockerBridge.postman_collection.json) for you to import in postman

## Sql

I already implemented an [SqlBridge](https://github.com/AskMeAgain/SqlBridge) to be used in combination with postman
to allow us to enter sql commands via rest. But we can do this with 
the DockerBridge too, by "execing" into the mysql container
and executing a shell command

![alt text](SqlBridge.png)

#### Note: We need to login in our DB (-u{{user}} -p{{password}})

Just add the following code to your Test Script in Postman, to visualize the
response correctly:

<details>
  <summary>Click here for the Test Script Code</summary>

    var template = `
    <link rel="stylesheet" href="https://pro.fontawesome.com/releases/v5.10.0/css/all.css" integrity="sha384-AYmEC3Yw5cVb3ZcuHtOA93w35dYTsvhLPVnYs9eStHfGJvOvKxVfELGroGkvsg+p" crossorigin="anonymous"/>
    <style>
        i {color: black }
        i:hover { color:grey; cursor: pointer; }
        i:active { color:black }
    </style>
    <script>
        function docker(containerId, command)
        {   
            var theUrl = 'http://localhost:8080/container/' + containerId + '/command';
            var xmlHttp = new XMLHttpRequest();
            xmlHttp.open( "POST", theUrl, false );
            xmlHttp.send(encodeURIComponent(command));
            var element = document.getElementById(containerId)
            element.textContent = xmlHttp.responseText;
        }
    </script>
    <table bgcolor="#FFFFFF">
    <tr>
        <th>Commands</th>
        <th>Image</th>
        <th>Id</th>
        <th>Status</th>
    </tr>
        {{#each containers}}
        <tr>
            <td style="text-align:center">
                <a onclick="docker('{{this.Id}}', 'start')"><i class="fas fa-play"></i></a>
                <a onclick="docker('{{this.Id}}', 'stop')"><i class="fas fa-stop-circle"></i></a>
                <a onclick="docker('{{this.Id}}', 'remove')"><i class="fas fa-trash"></i></a>
                    <a onclick="docker('{{this.Id}}', 'restart')"><i class="fas fa-sync-alt"></i></a>
            </td>
            <td>{{this.Image}}</td>
            <td>
                <span style="display:inline-block;
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: clip;
                    max-width: 8ch;">
                    {{this.Id}}
                </span>
            </td>    
            <td>
                <span id='{{this.Id}}'>{{this.State}}</span>
            </td>
        </tr>
        {{/each}}
    </table>
    `;

    var header = pm.response.json()[0];
    var data = pm.response.json().slice(1);
    
    pm.visualizer.set(template, {
        containers: pm.response.json()
    });

</details>

## [Portainer](https://www.portainer.io/) (lol)

Yes, since we have complete access to docker we can just run a (simple)
portainer version inside Postman:

![alt text](portainer.gif)

<details>
  <summary>Click here for the Test Script Code</summary>
    
    var template = `
    <link rel="stylesheet" href="https://pro.fontawesome.com/releases/v5.10.0/css/all.css" integrity="sha384-AYmEC3Yw5cVb3ZcuHtOA93w35dYTsvhLPVnYs9eStHfGJvOvKxVfELGroGkvsg+p" crossorigin="anonymous"/>
    <style>
        i {color: black }
        i:hover { color:grey; cursor: pointer; }
        i:active { color:black }
    </style>
    <script>
        function docker(containerId, command, newState)
        {   
            var theUrl = 'http://localhost:8080/container/' + containerId + '/' + command;
            var xmlHttp = new XMLHttpRequest();
            xmlHttp.open( "GET", theUrl, false ); // false for synchronous request
            xmlHttp.send(null);
            var element = document.getElementById(containerId)
            element.textContent = xmlHttp.responseText;
        }
    </script>
    <table bgcolor="#FFFFFF">
    <tr>
        <th>Commands</th>
        <th>Image</th>
        <th>Id</th>
        <th>Status</th>
    </tr>
        {{#each containers}}
        <tr>
            <td style="text-align:center">
                <a onclick="docker('{{this.Id}}', 'start')"><i class="fas fa-play"></i></a>
                <a onclick="docker('{{this.Id}}', 'stop')"><i class="fas fa-stop-circle"></i></a>
                <a onclick="docker('{{this.Id}}', 'remove')"><i class="fas fa-trash"></i></a>
                    <a onclick="docker('{{this.Id}}', 'restart')"><i class="fas fa-sync-alt"></i></a>
            </td>
            <td>{{this.Image}}</td>
            <td>
                <span style="display:inline-block;
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: clip;
                    max-width: 8ch;">
                    {{this.Id}}
                </span>
            </td>    
            <td>
                <span id='{{this.Id}}'>{{this.State}}</span>
            </td>
        </tr>
        {{/each}}
    </table>
    `;
    
    var header = pm.response.json()[0];
    var data = pm.response.json().slice(1);
    
    pm.visualizer.set(template, {
    containers: pm.response.json(),
    ownUrl: pm.request.url.toString()
    });

</details>

## Installation

1. add the [docker-java](https://github.com/docker-java/docker-java) 
dependency to your project
2. copy the [DockerBridgeController](/src/main/java/ask/me/again/dockerbridge/DockerBridgeController.java) into your project
3. (Optional) Import the example [PostmanCollection](/DockerBridge.postman_collection.json) into Postman
4. done