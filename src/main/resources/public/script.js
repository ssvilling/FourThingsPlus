async function login(){

    const username = document.getElementById("username").value;
    const password = document.getElementById("password").value;

    const response = await fetch("/login",{
        method:"POST",
        headers:{
            "Content-Type":"application/json"
        },
        body:JSON.stringify({username,password})
    });

    if(response.ok){
        window.location = "/index.html";
    }
    else{
        alert("Forkert login");
    }

}


async function createUser() {

    const username = document.getElementById("username").value;
    const password = document.getElementById("password").value;

    const response = await fetch("/signup", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({username, password})
    });

    if (response.ok) {
        alert("Bruger oprettet");
        window.location = "/login.html";
    } else {
        alert("Bruger findes allerede");
    }
}



    async function loadUser() {
        const res = await fetch("/me");

        if (res.ok) {
            const data = await res.json();
            document.getElementById("usernameDisplay").innerText = data.username;
        }
    }

async function addNote() {
    const content = document.getElementById("noteInput").value;

    const res = await fetch("/notes", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ content: content })
    });

    if (res.ok) {
        loadNotes();
    } else {
        alert("Fejl ved gemning");
    }
}

async function loadNotes() {
    const res = await fetch("/notes");

    if (res.ok) {
        const notes = await res.json();

        const doingList = document.getElementById("doingList");
        const doneList = document.getElementById("doneList");

        doingList.innerHTML = "";
        doneList.innerHTML = "";

        notes.forEach(note => {
            const li = document.createElement("li");

            li.innerText = note.content + " ";

            // Remove knap
            const removeBtn = document.createElement("button");
            removeBtn.innerText = "Remove";
            removeBtn.onclick = () => removeNote(note.id);

            li.appendChild(removeBtn);

            // Status knap
            const statusBtn = document.createElement("button");

            if (note.status === "DOING") {
                statusBtn.innerText = "Done";
                statusBtn.onclick = () => updateStatus(note.id, "DONE");
                doingList.appendChild(li);
            } else {
                statusBtn.innerText = "Doing";
                statusBtn.onclick = () => updateStatus(note.id, "DOING");
                doneList.appendChild(li);
            }

            li.appendChild(statusBtn);
        });
    }
}

async function updateStatus(id, status) {
    await fetch("/notes/status", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ id, status })
    });

    loadNotes();
}

async function removeNote(id) {
    await fetch("/notes/delete", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ id: id })
    });

    loadNotes();
}

function goToCreate(){
    window.location = "/signup.html";
}

function goToLogin(){
    window.location = "/login.html";
}
    async function logout() {
        await fetch("/logout");
        window.location = "/login.html";
}