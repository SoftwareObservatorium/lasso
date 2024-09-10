import * as React from 'react';
import { useNavigate } from 'react-router-dom';
import { Avatar, Button, Grid2, Link, Paper, TextField, Typography } from '@mui/material';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import FormControlLabel from '@mui/material/FormControlLabel';
import Checkbox from '@mui/material/Checkbox';
import LocationCityIcon from '@mui/icons-material/LocationCity';
import { useState } from 'react';
import AuthService from '../../services/AuthService';


function Login() {
    const navigate = useNavigate();

    const paperStyle = { padding: 20, height: '70vh', width: 280, margin: "19px auto", backgroundColor: '#E6F4F1', borderRadius: '12px', boxShadow: '0px 0px 8px rgba(0, 0, 0, 25)' }
    const avatarStyle = { backgroundColor: '#D9D9D9' }
    const btnstyle = { backgroundColor: '#1B6DA1', margin: '12px 0' }
    const logoStyle = { backgroundColor: '#D9D9D9', margin: '10px 0', width: 70, height: 70 }

    const [loading, setLoading] = useState(false);
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [message, setMessage] = useState("");

    const onChangeUsername = (e: any) => {
        const username = e.target.value;
        setUsername(username);
    };

    const onChangePassword = (e: any) => {
        const password = e.target.value;
        setPassword(password);
    };

    const handleLogin = (e: any) => {
        e.preventDefault();

        setMessage("");
        setLoading(true);

        //form.current.validateAll();
        const valid: boolean = true

        if (valid) {
            AuthService.login(username, password).then(
                () => {
                    navigate("/profile");
                    window.location.reload();
                },
                (error) => {
                    const resMessage =
                        (error.response &&
                            error.response.data &&
                            error.response.data.message) ||
                        error.message ||
                        error.toString();

                    setLoading(false);
                    setMessage(resMessage);
                }
            );
        } else {
            setLoading(false);
        }
    };


    return (

        <Grid2>
            <Grid2 >
                <Avatar style={logoStyle}><LocationCityIcon style={{ color: '#002A57', width: 56, height: 56 }} /></Avatar>
                <h2>Company Name</h2>
            </Grid2>


            <Paper >
                <Grid2 >
                    <Avatar style={avatarStyle}><LockOutlinedIcon style={{ color: '#002A57' }} /></Avatar>
                    <h2>Login</h2>
                </Grid2>
                <TextField id="standard-basic" label="Username" variant="standard" placeholder='Enter Your Username' fullWidth required onChange={onChangeUsername} value={username} />
                <TextField id="standard-basic" label="Password" variant="standard" placeholder='Enter Your Password' type='password' fullWidth required onChange={onChangePassword} value={password} />
                <FormControlLabel control={<Checkbox defaultChecked />} label="Remember Me" />

                <Button onClick={handleLogin} disabled={loading} style={btnstyle} type='submit' color='primary' variant="contained" fullWidth>Login

                {loading && (
                <span className="spinner-border spinner-border-sm"></span>
              )}
                </Button>
                <Typography>
                    <Link href="#" >
                        Forgot Password?
                    </Link>
                </Typography>

                <Typography>Don't have an account?
                    <Link href="#" >
                        Sign Up Here.
                    </Link>
                </Typography>
            </Paper>
        </Grid2>

    )
}

export default Login