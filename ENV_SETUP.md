# Environment Variables Setup

This project uses environment variables from a `.env` file for sensitive configuration like database credentials and email settings.

## Setup Instructions

1. **Create a `.env` file** in the `backend` directory (same level as `pom.xml`)

2. **Copy the following template** and fill in your actual values:

```env
# Database Configuration
DB_URL=jdbc:mysql://localhost:3306/andhra_machines?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USERNAME=your_database_username
DB_PASSWORD=your_database_password

# Email Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password

# Cloudinary Configuration
CLOUDINARY_CLOUD_NAME=your_cloudinary_cloud_name
CLOUDINARY_API_KEY=your_cloudinary_api_key
CLOUDINARY_API_SECRET=your_cloudinary_api_secret

# Admin Contact Information
ADMIN_EMAIL=your_admin_email@gmail.com
ADMIN_PHONE=+91 8328657726

# JWT Configuration
JWT_SECRET=your_jwt_secret_key_minimum_256_bits_long_for_security
JWT_EXPIRATION=86400000
```

3. **Replace the placeholder values:**
   - `DB_USERNAME`: Your MySQL database username
   - `DB_PASSWORD`: Your MySQL database password
   - `MAIL_USERNAME`: Your Gmail address (or other SMTP email)
   - `MAIL_PASSWORD`: Your Gmail App Password (not your regular password)
   - `CLOUDINARY_CLOUD_NAME`: Your Cloudinary cloud name
   - `CLOUDINARY_API_KEY`: Your Cloudinary API key
   - `CLOUDINARY_API_SECRET`: Your Cloudinary API secret
   - `JWT_SECRET`: A secure random string (minimum 256 bits/32 characters) for signing JWT tokens
   - `JWT_EXPIRATION`: Token expiration time in milliseconds (86400000 = 24 hours)

## Gmail App Password Setup

If using Gmail, you need to create an App Password:

1. Go to your Google Account settings
2. Enable 2-Step Verification
3. Go to App Passwords
4. Generate a new app password for "Mail"
5. Use that 16-character password in `MAIL_PASSWORD`

## Cloudinary Setup

To get your Cloudinary credentials:

1. Sign up or log in to [Cloudinary](https://cloudinary.com/)
2. Go to your Dashboard
3. Copy the following values:
   - **Cloud Name**: Found in the Dashboard URL or Account Details
   - **API Key**: Found in the Dashboard
   - **API Secret**: Found in the Dashboard (click "Reveal" to see it)

## Important Notes

- The `.env` file is already in `.gitignore` and will NOT be committed to version control
- Never commit your `.env` file with real credentials
- The application will fail to start if required environment variables are not set
- Make sure the `.env` file is in the `backend` directory (root of the backend project)

## Verification

After creating the `.env` file, restart your Spring Boot application. You should see:
```
Environment variables loaded from .env file
```

If you see a warning message, check that:
- The `.env` file exists in the correct location
- The file has the correct variable names (case-sensitive)
- There are no extra spaces around the `=` sign

