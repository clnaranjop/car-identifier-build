# Embedding Regeneration Status
**Date:** October 30, 2025, 21:27
**Status:** IN PROGRESS ✅

---

## Background

After cleaning 162 problematic images from the reference database (images with extreme aspect ratios indicating blister packs shown vertically/horizontally), the app was still showing poor identification results (28% match with blister images).

**Root Cause:** The embeddings files (`embeddings_mobilenetv3.json` and `.npz`) still contained OLD embeddings from the original 10,849 images, including the 162 problematic images we removed.

**Solution:** Regenerate embeddings using only the cleaned 10,687 images.

---

## Current Progress

**Script:** `regenerate_embeddings.py`
**Process:** Running in background (unbuffered mode)
**Log File:** `embedding_generation.log`

**Images to Process:** 10,687
**Current Status:** Processing (~400 images processed in first minute)
**Estimated Time:** ~12-15 minutes (much faster than expected!)

**Performance:**
- CPU: ~143% (utilizing multiple cores)
- Processing rate: ~800 images/minute
- Model: MobileNetV3 ONNX (17MB)
- Runtime: ONNX Runtime (CPU only)

---

## Technical Details

**Image Preprocessing:**
- Resize to 224x224 pixels
- ImageNet normalization (mean: [0.485, 0.456, 0.406], std: [0.229, 0.224, 0.225])
- Convert to NCHW format (batch, channels, height, width)
- Float32 data type

**Output Files:**
1. `embeddings_mobilenetv3_new.json` - JSON format (expected ~293MB)
2. `embeddings_mobilenetv3_new.npz` - Numpy compressed (expected ~55MB)

---

## Next Steps (After Completion)

### 1. Backup Old Embeddings
```bash
mv app/src/main/assets/embeddings_mobilenetv3.json app/src/main/assets/embeddings_mobilenetv3_old.json
mv app/src/main/assets/embeddings_mobilenetv3.npz app/src/main/assets/embeddings_mobilenetv3_old.npz
```

### 2. Install New Embeddings
```bash
mv app/src/main/assets/embeddings_mobilenetv3_new.json app/src/main/assets/embeddings_mobilenetv3.json
mv app/src/main/assets/embeddings_mobilenetv3_new.npz app/src/main/assets/embeddings_mobilenetv3.npz
```

### 3. Recompile App
```bash
./gradlew assembleDebug
```

### 4. Install on Device
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 5. Test and Verify
- Take photo of same car that showed 28% match
- Verify improvement in match percentage (expected: 50-80%)
- Confirm no blister images in top results
- Take screenshot to compare results

---

## Expected Improvements

### Before Regeneration:
- ❌ Match confidence: 28-30%
- ❌ Blister images appearing in top results
- ❌ Sideways/rotated images confusing identification
- ❌ Old embeddings from 10,849 images (including 162 problematic ones)

### After Regeneration:
- ✅ Better match confidence (expected 50-80%)
- ✅ Clean embeddings from 10,687 quality images only
- ✅ No extreme aspect ratio images
- ✅ More consistent orientation
- ✅ Improved accuracy for user identification

---

## Files Involved

**Input:**
- `app/src/main/assets/mobilenetv3_embeddings.onnx` (17MB) - ML model
- `app/src/main/assets/reference_images/**/*.jpg` (10,687 images) - cleaned reference images
- `app/src/main/assets/hotwheels_models.json` (5.2MB) - model database

**Output:**
- `embeddings_mobilenetv3_new.json` - new JSON embeddings
- `embeddings_mobilenetv3_new.npz` - new compressed embeddings
- `embedding_generation.log` - process log

**Old (to be backed up):**
- `embeddings_mobilenetv3.json` (293MB) - old embeddings from 10,849 images
- `embeddings_mobilenetv3.npz` (55MB) - old compressed embeddings

---

## Monitoring Progress

Check progress with:
```bash
tail -f embedding_generation.log
```

Or check completion:
```bash
ls -lh app/src/main/assets/embeddings_mobilenetv3_new.*
```

---

## Troubleshooting

**If process stops:**
```bash
# Check if running
ps aux | grep regenerate_embeddings

# Restart if needed
python3 -u regenerate_embeddings.py > embedding_generation.log 2>&1 &
```

**If out of memory:**
- Close other applications
- Check with: `free -h`
- Script is CPU-only, minimal memory usage expected

**If errors occur:**
- Check log: `cat embedding_generation.log`
- Check Python packages: `pip3 list | grep -E "onnxruntime|pillow|numpy"`

---

## Why This Fixes the Problem

The app performs identification by:
1. Taking user's photo
2. Generating embedding for that photo using MobileNetV3
3. **Comparing against ALL embeddings in the database**
4. Finding closest matches by vector similarity

Even though we removed 162 problematic images from `reference_images/`, the **embeddings file still contained those 162 old embeddings**. So the app was still matching against the old blister pack images!

By regenerating embeddings with ONLY the cleaned 10,687 images, we ensure:
- Only quality images are in the embedding database
- No blister packs with extreme aspect ratios
- Better orientation consistency
- Improved match accuracy

---

## Timeline

- **21:27** - Started embedding regeneration
- **~21:40** - Expected completion (13 minutes estimated)
- **21:45** - Backup and install new embeddings
- **22:00** - Compile and test app with new embeddings

---

**Status:** Process running smoothly. Will complete in approximately 10-15 minutes.
