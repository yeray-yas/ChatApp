package com.yerayyas.chatappkotlinproject.notifications

class Data {

    private var user : String = ""
    private var icon = 0
    private var body : String = ""
    private var title : String = ""
    private var sent : String = ""

    constructor(){

    }

    constructor(user: String, icon: Int, body: String, title: String, sent: String) {
        this.user = user
        this.icon = icon
        this.body = body
        this.title = title
        this.sent = sent
    }


    fun getUser(): String{
        return user
    }

    fun setUser(usuario: String){
        this.user = usuario
    }

    fun getIcon(): Int{
        return icon
    }

    fun setIcon(icon: Int){
        this.icon = icon
    }

    fun getBody(): String?{
        return body
    }

    fun setBody(body: String){
        this.body = body
    }

    fun getTitle(): String?{
        return title
    }

    fun setTitle(title: String){
        this.title = title
    }

    fun getSent(): String?{
        return sent
    }

    fun setSent(sent: String){
        this.sent = sent
    }

}