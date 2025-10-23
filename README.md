class ImageGridAdapter(
    private val onCellClick: (position: Int) -> Unit,
    private val onSwapImageOnly: (from: Int, to: Int) -> Unit // callback về Fragment để cập nhật data
) : ListAdapter<GridItem, ImageGridAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<GridItem>() {
        override fun areItemsTheSame(o: GridItem, n: GridItem) = o.id == n.id
        override fun areContentsTheSame(o: GridItem, n: GridItem) = o.uri == n.uri
    }

    inner class VH(val root: View) : RecyclerView.ViewHolder(root) {
        val img: ImageView = root.findViewById(R.id.image)
        val overlay: View = root.findViewById(R.id.dragOverlay)
        val card: CardView = root.findViewById(R.id.card)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_image_cell, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val item = getItem(position)

        // render ảnh
        if (item.uri != null) {
            h.img.setImageURI(item.uri)
        } else {
            h.img.setImageDrawable(null)
            h.img.setBackgroundColor(Color.parseColor("#11000000"))
        }

        // click để chọn/đổi ảnh bình thường
        h.root.setOnClickListener { onCellClick(h.bindingAdapterPosition) }

        // LONG CLICK => bắt đầu kéo (chỉ nếu ô có ảnh)
        if (item.uri != null) {
            h.root.setOnLongClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

                // hiệu ứng tại chỗ (không di chuyển view)
                h.overlay.visibility = View.VISIBLE
                h.card.cardElevation = v.resources.displayMetrics.density * 8
                h.root.animate().scaleX(1.04f).scaleY(1.04f).setDuration(100).start()

                // payload: fromPosition
                val clip = ClipData.newPlainText("fromPos", h.bindingAdapterPosition.toString())
                val shadow = ImageDragShadow(h.img)

                // localState = fromPosition để target đọc
                v.startDragAndDrop(clip, shadow, h.bindingAdapterPosition, 0)

                true
            }
        } else {
            h.root.setOnLongClickListener(null)
        }

        // DRAG LISTENER: đặt cho từng ô
        h.root.setOnDragListener { view, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    // Chỉ chấp nhận drag nếu payload hợp lệ
                    val hasText = event.clipData != null && event.clipData.itemCount > 0
                    view.isEnabled = hasText
                    hasText
                }

                DragEvent.ACTION_DRAG_ENTERED -> {
                    // highlight ô đích nếu ĐÍCH có ảnh (chính sách: chỉ swap khi cả 2 có ảnh)
                    val targetItem = currentList.getOrNull(h.bindingAdapterPosition)
                    if (targetItem?.uri != null) {
                        h.overlay.visibility = View.VISIBLE
                        h.root.animate().alpha(0.92f).setDuration(80).start()
                    }
                    true
                }

                DragEvent.ACTION_DRAG_EXITED -> {
                    if (h.bindingAdapterPosition != (event.localState as? Int)) {
                        h.overlay.visibility = View.GONE
                        h.root.animate().alpha(1f).setDuration(80).start()
                    }
                    true
                }

                DragEvent.ACTION_DROP -> {
                    val from = event.localState as? Int ?: return@setOnDragListener false
                    val to = h.bindingAdapterPosition
                    val fromItem = currentList.getOrNull(from)
                    val toItem = currentList.getOrNull(to)

                    // Chính sách: chỉ đổi ảnh khi cả 2 ô đều có ảnh
                    val allowed = (from != to) && (fromItem?.uri != null) && (toItem?.uri != null)
                    if (allowed) {
                        // Gọi callback để SWAP URI (không đổi vị trí view)
                        onSwapImageOnly(from, to)
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        true
                    } else {
                        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                        false
                    }
                }

                DragEvent.ACTION_DRAG_ENDED -> {
                    // clear mọi hiệu ứng
                    h.overlay.visibility = View.GONE
                    h.root.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(100).start()
                    h.card.cardElevation = 0f
                    true
                }

                else -> false
            }
        }

        // reset về mặc định mỗi lần bind
        h.overlay.visibility = View.GONE
        h.root.scaleX = 1f
        h.root.scaleY = 1f
        h.root.alpha = 1f
        h.card.cardElevation = 0f
    }
}
class ImageGridFragment : Fragment(R.layout.fragment_image_grid) {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ImageGridAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recycler = view.findViewById(R.id.recycler)

        adapter = ImageGridAdapter(
            onCellClick = { pos ->
                // mở picker/camera để set ảnh cho pos
            },
            onSwapImageOnly = { from, to ->
                swapUrisOnly(from, to) // cập nhật list & submit
            }
        )

        recycler.layoutManager = GridLayoutManager(requireContext(), 3)
        recycler.setHasFixedSize(true)
        recycler.adapter = adapter
        recycler.addItemDecoration(GridSpacingDecoration(3, dp(6), includeEdge = true))

        // init 9 ô
        val init = (0 until 9).map { GridItem(id = it, uri = null) }
        adapter.submitList(init)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    /** Chỉ đổi URI giữa 2 vị trí (view không đổi chỗ) */
    private fun swapUrisOnly(from: Int, to: Int) {
        val cur = adapter.currentList
        if (from !in cur.indices || to !in cur.indices) return
        val a = cur[from]
        val b = cur[to]
        if (a.uri == null || b.uri == null) return

        val new = cur.toMutableList()
        new[from] = a.copy(uri = b.uri)
        new[to] = b.copy(uri = a.uri)
        adapter.submitList(new)
    }
}


