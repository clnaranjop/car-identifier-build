# CUDA Setup Guide for GeForce MX130

## Hardware Information

**GPU:** NVIDIA GeForce MX130
- Architecture: Maxwell (GM108M)
- Compute Capability: 5.0
- CUDA Cores: 384
- VRAM: 2GB GDDR5
- TDP: 30W

**Compatibility:**
- ✅ CUDA 11.8 (last version supporting Maxwell)
- ✅ Driver 470.x series
- ✅ PyTorch 2.0+ with CUDA 11.8
- ✅ TensorFlow 2.12+

---

## Installation Steps

### Option 1: Automated Installation (Recommended)

1. **Run the installation script:**
```bash
cd /home/cristhyan/Escritorio/proy_h
chmod +x install_cuda.sh
./install_cuda.sh
```

2. **Reboot your system:**
```bash
sudo reboot
```

3. **Verify installation:**
```bash
nvidia-smi
nvcc --version
```

---

### Option 2: Manual Installation

#### Step 1: Install NVIDIA Drivers

```bash
# Add NVIDIA PPA
sudo add-apt-repository ppa:graphics-drivers/ppa
sudo apt update

# Install driver 470 (stable for MX130)
sudo apt install nvidia-driver-470

# Reboot
sudo reboot
```

#### Step 2: Download CUDA 11.8

```bash
cd /tmp
wget https://developer.download.nvidia.com/compute/cuda/11.8.0/local_installers/cuda_11.8.0_520.61.05_linux.run
```

#### Step 3: Install CUDA

```bash
sudo sh cuda_11.8.0_520.61.05_linux.run --silent --toolkit --no-opengl-libs
```

#### Step 4: Configure Environment

Add to `~/.bashrc`:
```bash
export PATH=/usr/local/cuda-11.8/bin:$PATH
export LD_LIBRARY_PATH=/usr/local/cuda-11.8/lib64:$LD_LIBRARY_PATH
export CUDA_HOME=/usr/local/cuda-11.8
```

Apply changes:
```bash
source ~/.bashrc
```

---

## Python/PyTorch Setup

### Install PyTorch with CUDA 11.8

```bash
pip3 install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu118
```

### Verify PyTorch can see GPU

```python
import torch
print(f"CUDA available: {torch.cuda.is_available()}")
print(f"CUDA device: {torch.cuda.get_device_name(0)}")
print(f"CUDA version: {torch.version.cuda}")
```

Expected output:
```
CUDA available: True
CUDA device: NVIDIA GeForce MX130
CUDA version: 11.8
```

---

## Testing CUDA

### Test 1: nvidia-smi

```bash
nvidia-smi
```

Should show:
- Driver version
- CUDA version
- GPU name (GeForce MX130)
- Memory usage

### Test 2: CUDA Sample

```bash
cd /usr/local/cuda-11.8/samples/1_Utilities/deviceQuery
sudo make
./deviceQuery
```

Should show detailed GPU info.

### Test 3: PyTorch Benchmark

```python
import torch
import time

# Create tensors on GPU
x = torch.rand(5000, 5000).cuda()
y = torch.rand(5000, 5000).cuda()

# Benchmark
start = time.time()
z = torch.matmul(x, y)
torch.cuda.synchronize()
end = time.time()

print(f"Matrix multiplication took: {end-start:.3f}s")
```

---

## Performance Expectations

### MX130 Limitations

⚠️ **This is an entry-level GPU:**
- 384 CUDA cores (vs 4352 in RTX 3070)
- 2GB VRAM (very limited)
- Maxwell architecture (older)

### Realistic Use Cases

✅ **Good for:**
- Learning CUDA programming
- Small dataset training
- Image processing (1-2 images at a time)
- Inference on small models
- Prototyping

❌ **NOT good for:**
- Training large models
- Batch processing large images
- Real-time video processing
- Production deep learning

### Expected Speedup vs CPU

For this HotWheels project:
- Image preprocessing: **2-3x faster**
- Embedding generation: **2-4x faster**
- Model training: **3-5x faster** (but still slow)

**CPU (Intel i5-8250U) vs MX130:**
- 1000 images processing: CPU ~30min, GPU ~12min
- Training small model: CPU ~2hrs, GPU ~45min

---

## For This Project (HotWheels Identifier)

### What CUDA Will Help With:

1. **Regenerating Embeddings:**
   ```python
   # Process 10,687 images with ONNX + CUDA
   # CPU: ~2-3 hours
   # GPU: ~45-60 minutes
   ```

2. **Image Preprocessing:**
   ```python
   # Batch resize/crop 10,000+ images
   # CPU: ~15 minutes
   # GPU: ~5 minutes
   ```

3. **Model Fine-tuning (if you want to retrain):**
   ```python
   # Train MobileNetV3 on 10,687 images
   # CPU: Days
   # GPU: Hours (but limited by 2GB VRAM)
   ```

### What CUDA WON'T Help With:

❌ Android app performance (runs on phone)
❌ APK compilation (CPU-bound)
❌ Database queries (CPU-bound)
❌ App installation (not GPU-related)

---

## Troubleshooting

### Problem: nvidia-smi not found

**Solution:**
```bash
# Reboot first
sudo reboot

# If still not working, reinstall driver
sudo apt install --reinstall nvidia-driver-470
sudo reboot
```

### Problem: CUDA version mismatch

**Solution:**
```bash
# Check versions
nvidia-smi  # Shows max CUDA version supported
nvcc --version  # Shows installed CUDA version

# Must match or CUDA < driver version
```

### Problem: Out of memory errors

**Solution:**
```python
# Reduce batch size
batch_size = 1  # Start small with 2GB VRAM

# Clear cache frequently
torch.cuda.empty_cache()

# Monitor memory
print(torch.cuda.memory_allocated() / 1024**2, "MB")
```

### Problem: Slow performance

**Causes:**
- Data transfer CPU↔GPU is slow
- Small batch sizes don't utilize GPU well
- MX130 is entry-level

**Solutions:**
- Keep data on GPU longer
- Increase batch size if possible
- Use mixed precision (FP16)

---

## Uninstallation (if needed)

```bash
# Remove CUDA
sudo /usr/local/cuda-11.8/bin/cuda-uninstaller

# Remove drivers
sudo apt remove --purge nvidia-*
sudo apt autoremove

# Reboot
sudo reboot
```

---

## Next Steps After Installation

1. **Reboot system**
2. **Verify with `nvidia-smi`**
3. **Install PyTorch** with CUDA support
4. **Run test script** to confirm GPU works
5. **Use for:**
   - Regenerating embeddings faster
   - Image processing batches
   - Future model training

---

## Summary

**Installation Time:** ~30-45 minutes
**Disk Space:** ~5GB
**Reboot Required:** Yes
**Complexity:** Medium

**Worth it for this project?**
- If you plan to regenerate embeddings: **Yes**
- If you plan to retrain models: **Maybe** (limited by 2GB)
- Just for the Android app: **No** (app runs on phone)

**My recommendation:** Install it since you have the GPU. It's free performance for future work, even if modest.
