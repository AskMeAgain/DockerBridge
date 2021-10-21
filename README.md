# DockerBridge

This is the ultimate answer to "what should not be done with postman?"

This bridge allows us to enter docker commands via rest. 
This itself doesnt sound that bad, but in combination
with Postman, we can do some insane stuff


## Sql

I already implemented an SqlBridge to be used in combination with postman
to allow us to enter sql commands via rest. But we can do this with 
this DockerBridge too:

![alt text](SqlBridge.png)

#### Note: We need to login in our DB

Just add the following code to your Test Script in Postman, to vizualize the
response correctly:

<details>
  <summary>Test Script Code</summary>
    
    var template = `
    <table bgcolor="#FFFFFF">
        <tr>
        {{#each header}}
            <th>{{this}}</th>
        {{/each}}
        </tr>
        {{#each data}}
        <tr>
        {{#each this}}
            <td>{{this}}</td>
        {{/each}}
        </tr>
        {{/each}}
    </table>
    `;
    
    var responseByLine = pm.response.text().split(/\r?\n/);
    
    var arr = new Array();
    
    for(var i = 1; i < responseByLine.length -1; i++){
        arr.push(responseByLine[i].split("\t"));
    }
    
    pm.visualizer.set(template, {
        header: responseByLine[0].split("\t"),
        data: arr
    });

</details>

## Portainer (lol)

Yes, since we have complete access to docker, we can just run a (simple)
portainer version:

