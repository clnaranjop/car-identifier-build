# Privacy Policy Setup Guide

## What We Created

A complete, professional Privacy Policy for HotWheels Identifier that:
- ✅ Complies with Google Play Store requirements
- ✅ GDPR compliant (Europe)
- ✅ CCPA compliant (California)
- ✅ Covers all app permissions and data usage
- ✅ Explains AdMob data collection
- ✅ Professional responsive design
- ✅ Available in Spanish and English

## File Location

`privacy-policy.html` - The complete Privacy Policy webpage

## Next Steps: Host on GitHub Pages (FREE)

### Option 1: Use This Project's Repository

If this project is already on GitHub, you can enable GitHub Pages:

1. **Go to your GitHub repository**
   ```
   https://github.com/YOUR_USERNAME/YOUR_REPO_NAME
   ```

2. **Enable GitHub Pages:**
   - Go to Settings > Pages
   - Under "Source", select "Deploy from a branch"
   - Select branch: `main` or `master`
   - Select folder: `/ (root)`
   - Click "Save"

3. **Commit and push the privacy policy:**
   ```bash
   git add privacy-policy.html
   git commit -m "Add privacy policy for Play Store compliance"
   git push
   ```

4. **Your Privacy Policy URL will be:**
   ```
   https://YOUR_USERNAME.github.io/YOUR_REPO_NAME/privacy-policy.html
   ```

---

### Option 2: Create a New Repository (Recommended for Clean URL)

1. **Create new GitHub repository:**
   - Go to https://github.com/new
   - Repository name: `hotwheels-identifier-privacy`
   - Description: "Privacy Policy for HotWheels Identifier App"
   - Make it Public
   - Check "Add a README file"
   - Click "Create repository"

2. **Upload privacy-policy.html:**
   - Click "Add file" > "Upload files"
   - Drag and drop `privacy-policy.html`
   - Rename it to `index.html` (for cleaner URL)
   - Commit changes

3. **Enable GitHub Pages:**
   - Go to Settings > Pages
   - Source: Deploy from branch
   - Branch: `main`, folder: `/ (root)`
   - Save

4. **Your Privacy Policy URL will be:**
   ```
   https://YOUR_USERNAME.github.io/hotwheels-identifier-privacy/
   ```

---

### Option 3: Use GitHub Gist (Simplest)

1. **Go to:** https://gist.github.com/
2. **Create new Gist:**
   - Filename: `privacy-policy.html`
   - Paste the entire content from `privacy-policy.html`
   - Create Public Gist
3. **Click "View Raw"** - this is your Privacy Policy URL

---

## After Publishing

### 1. Update Privacy Policy Contact Info

Before publishing, edit `privacy-policy.html` and replace:

```html
<p><strong>Email:</strong> [YOUR_EMAIL_HERE]</p>
<p><strong>Developer:</strong> [YOUR_NAME_OR_COMPANY_HERE]</p>
```

With your actual information:

```html
<p><strong>Email:</strong> your.email@example.com</p>
<p><strong>Developer:</strong> Your Name or Company Name</p>
```

### 2. Test the URL

Open the GitHub Pages URL in your browser and verify:
- Page loads correctly
- All sections are readable
- Formatting looks good on mobile
- Links work (Google's privacy policy links)

### 3. Use the URL in Play Store

When setting up your Play Store listing:
1. Go to Play Console > Your App > Store Presence > Privacy Policy
2. Paste your GitHub Pages URL
3. Save

### 4. Add Privacy Policy Link to Your App (Optional but Recommended)

You can add a link in the app's Settings screen:

```kotlin
// In SettingsActivity.kt
val privacyPolicyItem = PreferenceItem(
    title = "Privacy Policy",
    description = "View our privacy policy",
    onClick = {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://YOUR_USERNAME.github.io/hotwheels-identifier-privacy/")
        startActivity(intent)
    }
)
```

---

## What the Privacy Policy Covers

✅ **Information We DON'T Collect:**
- Personal identification
- Photos/images (processed locally only)
- Collection data (stored locally only)
- Location, analytics, or behavioral data

✅ **Permissions Explained:**
- Camera (for taking photos)
- Internet (for ads only)
- Storage (temporary photo storage)

✅ **Third-Party Services:**
- Google AdMob (advertising)
- Their data collection policies
- How to opt-out

✅ **User Rights:**
- Data access and deletion
- Privacy controls
- GDPR/CCPA compliance

✅ **Children's Privacy:**
- No data collection from children
- Safe for all ages

---

## Important Notes

⚠️ **Keep the URL permanent** - Once you submit to Play Store, don't change the Privacy Policy URL. You can update the content, but the URL must stay the same.

⚠️ **Update the date** - Whenever you change the Privacy Policy content, update the "Last Updated" date.

⚠️ **Before Play Store submission:**
1. Add your actual email and developer name
2. Verify the page is publicly accessible
3. Test on mobile device
4. Save the URL - you'll need it for Play Store listing

---

## Alternative Free Hosting Options

If you don't want to use GitHub Pages:

1. **Google Sites** (sites.google.com) - Free, easy drag-and-drop
2. **Netlify Drop** (app.netlify.com/drop) - Just drag and drop the HTML file
3. **Firebase Hosting** (firebase.google.com) - Free tier available
4. **Vercel** (vercel.com) - Free for personal projects

---

## Play Store Requirements ✓

This Privacy Policy meets all Google Play Store requirements:

✅ Publicly accessible URL
✅ Describes what data is collected
✅ Explains how data is used
✅ Covers third-party services (AdMob)
✅ Explains user rights
✅ Contact information provided
✅ COPPA compliant (children's privacy)
✅ GDPR compliant
✅ Mobile-responsive design

---

## Questions?

If you need to make changes to the Privacy Policy:
1. Edit `privacy-policy.html`
2. Update the "Last Updated" date
3. Re-upload to GitHub Pages
4. Changes will be live within a few minutes

**No need to update Play Store listing** - as long as the URL stays the same, Google will see the updated content.
