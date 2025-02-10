import { Button, List, ListItem, ListItemIcon, ListItemText, Typography } from '@mui/material';
import Grid from '@mui/material/Grid';
import React, { useState } from 'react';
import { Examples } from '../model/examples';

import AnalyticsIcon from '@mui/icons-material/Analytics';
import { createSearchParams, useNavigate } from 'react-router-dom';

const ExamplePage = () => {
  const navigate = useNavigate();

  let examples = Object.keys(Examples.MAP);

  const humanLabel = (exampleId: string) => {
    return Examples.MAP[exampleId as keyof typeof Examples.MAP].label;
  };

  const handleExampleClick = (exampleId: string) => {
    navigate({
      pathname: "../editor",
      search: createSearchParams({ example: exampleId }).toString()
  });
  }
  
  return (
    <React.Fragment>
              <Grid item xs={12} md={6}>
          <Typography sx={{ mt: 4, mb: 2 }} variant="h6" component="div">
            Examples
          </Typography>
          <List dense={false}>
              {examples.map((key) => (
                <ListItem>
                  <ListItemIcon>
                    <AnalyticsIcon />
                  </ListItemIcon>
                  <Button onClick={(event) => handleExampleClick(key)}>
                  <ListItemText
                    primary={humanLabel(key)}
                    secondary={key}
                  />
                  </Button>
                </ListItem>
                ))
                }
            </List>
        </Grid>

    </React.Fragment>
  );
}

export default ExamplePage;
