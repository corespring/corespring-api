/**
 * To passin in the user password call the script like so:
 * mongo my_db --eval 'var user= "..."; var password = "...."'
 */
if(!user || !password){
  throw "You need so specify a user and password"
}

db.addUser(user, password);