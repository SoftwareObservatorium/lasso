import { Button, Card, CardActions, CardContent, Typography } from '@mui/material';
import React from 'react';

function AboutPage() {
  return (
    <div className="App">
      <h1>About</h1>

      <Card sx={{ maxWidth: 345 }}>
      <CardContent>
        <Typography gutterBottom variant="h5" component="div">
        Author
        </Typography>
        <Typography variant="body2" sx={{ color: 'text.secondary' }}>
          Marcus Kessel (marcus.kessel@uni-mannheim.de), Chair of Software Engineering, University of Mannheim
        </Typography>
      </CardContent>
      <CardActions>
        <Button size="small">Contact</Button>
      </CardActions>
    </Card>
    </div>
  );
}

export default AboutPage;
