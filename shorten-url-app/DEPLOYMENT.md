# Firebase Deployment Guide

## Prerequisites

1. **MongoDB Atlas** - You need a MongoDB Atlas cluster for production
   - Create a free cluster at https://www.mongodb.com/cloud/atlas
   - Get your connection string
   - Update `functions/.env` with your MongoDB Atlas URI

2. **Firebase Project** - Already configured: `link-shortner-e370c`

## Environment Setup

1. Update `functions/.env` with your production MongoDB URI:

   ```
   MONGO_URI=mongodb+srv://<username>:<password>@<cluster>.mongodb.net/<database>?retryWrites=true&w=majority
   ```

2. Set Firebase environment variables:
   ```bash
   cd functions
   firebase functions:config:set \
     auth0.audience="https://link-shortener-api" \
     auth0.issuer="https://dev-py1e5k3snqd8axx2.eu.auth0.com/" \
     mongodb.uri="your_mongodb_atlas_connection_string"
   ```

## Deployment Steps

### 1. Deploy Functions

```bash
# From project root
firebase deploy --only functions
```

Your API will be available at:

```
https://us-central1-link-shortner-e370c.cloudfunctions.net/api
```

### 2. Update Frontend Environment

Update your frontend `.env` file with the Firebase Function URL:

```
VITE_API_BASE_URL=https://us-central1-link-shortner-e370c.cloudfunctions.net/api
```

### 3. Test the Deployment

```bash
# Test the API endpoint
curl https://us-central1-link-shortner-e370c.cloudfunctions.net/api
```

## Local Testing

Test Firebase Functions locally:

```bash
# Start the emulator
firebase emulators:start

# Your API will be available at:
# http://localhost:5001/link-shortner-e370c/us-central1/api
```

## Troubleshooting

### View Logs

```bash
firebase functions:log
```

### Check Deployment Status

```bash
firebase functions:list
```

### Common Issues

1. **Database Connection Errors**
   - Ensure your MongoDB Atlas connection string is correct
   - Whitelist Firebase IP addresses in MongoDB Atlas (0.0.0.0/0 for all IPs)

2. **Auth0 Issues**
   - Verify Auth0 environment variables are set correctly
   - Check Auth0 allowed callback URLs include your Firebase Function URL

3. **CORS Issues**
   - Update Auth0 allowed origins with your deployed function URL
   - Ensure CORS is properly configured in the Express app

## Firebase Function URL Structure

Your endpoints will be:

- Base URL: `https://us-central1-link-shortner-e370c.cloudfunctions.net/api`
- Get Links: `GET /api/links`
- Create Link: `POST /api/link`
- Get Link: `GET /api/links/:id`
- Delete Link: `DELETE /api/links/:id`
- Redirect: `GET /:shortCode`

## Cost Considerations

Firebase Free Tier includes:

- 2 million invocations/month
- 400,000 GB-seconds of compute time
- 200,000 CPU-seconds of compute time
- 5GB network egress

Monitor usage at: https://console.firebase.google.com/project/link-shortner-e370c/usage
