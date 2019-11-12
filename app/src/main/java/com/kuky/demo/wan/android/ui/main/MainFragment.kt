package com.kuky.demo.wan.android.ui.main

import android.os.Bundle
import android.view.View
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.kuky.demo.wan.android.R
import com.kuky.demo.wan.android.base.BaseFragment
import com.kuky.demo.wan.android.base.BaseFragmentPagerAdapter
import com.kuky.demo.wan.android.data.PreferencesHelper
import com.kuky.demo.wan.android.databinding.FragmentMainBinding
import com.kuky.demo.wan.android.databinding.UserProfileHeaderBinding
import com.kuky.demo.wan.android.ui.collection.CollectionFragment
import com.kuky.demo.wan.android.ui.dialog.AboutUsDialog
import com.kuky.demo.wan.android.ui.dialog.LoginDialogFragment
import com.kuky.demo.wan.android.ui.dialog.WxDialog
import com.kuky.demo.wan.android.ui.home.HomeArticleFragment
import com.kuky.demo.wan.android.ui.hotproject.HotProjectFragment
import com.kuky.demo.wan.android.ui.system.KnowledgeSystemFragment
import com.kuky.demo.wan.android.ui.websitedetail.WebsiteDetailFragment
import com.kuky.demo.wan.android.ui.wxchapter.WxChapterFragment
import com.kuky.demo.wan.android.utils.ApplicationUtils
import com.kuky.demo.wan.android.utils.GalleryTransformer
import com.kuky.demo.wan.android.utils.ScreenUtils
import com.youth.banner.listener.OnBannerListener
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.toast
import org.jetbrains.anko.yesButton

/**
 * @author kuky.
 * @description 主页面 fragment 持有者
 */
class MainFragment : BaseFragment<FragmentMainBinding>() {

    private val mAdapter: BaseFragmentPagerAdapter by lazy {
        BaseFragmentPagerAdapter(
            childFragmentManager, arrayListOf(
                HomeArticleFragment(),
                HotProjectFragment(),
                KnowledgeSystemFragment(),
                WxChapterFragment()
            )
        )
    }

    private val mViewModel: MainViewModel by lazy {
        ViewModelProvider(requireActivity(), MainModelFactory(MainRepository()))
            .get(MainViewModel::class.java)
    }

    override fun getLayoutId(): Int = R.layout.fragment_main

    override fun initFragment(view: View, savedInstanceState: Bundle?) {
        mBinding.holder = this@MainFragment
        mBinding.viewModel = mViewModel
        mBinding.listener = OnBannerListener { position ->
            mViewModel.banners.value?.let {
                WebsiteDetailFragment.viewDetail(
                    mNavController,
                    R.id.action_mainFragment_to_websiteDetailFragment,
                    it[position].url
                )
            }
        }

        mBinding.banner.let {
            it.layoutParams = it.layoutParams.apply {
                width = ScreenUtils.getScreenWidth(requireContext())
                height = (width * 0.45f).toInt()
            }
        }

        // Bind NavigationView Header Layout
        val headerBinding = DataBindingUtil.inflate<UserProfileHeaderBinding>(
            layoutInflater, R.layout.user_profile_header, mBinding.userProfileDrawer, false
        )
        headerBinding.holder = this@MainFragment
        mBinding.userProfileDrawer.addHeaderView(headerBinding.root)

        // Bind ViewPager
        mBinding.adapter = mAdapter
        mBinding.limit = mAdapter.count
        mBinding.transformer = GalleryTransformer()

        mViewModel.getBanners()

        mViewModel.hasLogin.observe(this, Observer<Boolean> {
            val menus = mBinding.userProfileDrawer.menu

            menus.findItem(R.id.user_collections).isVisible = it
            menus.findItem(R.id.login_out).isVisible = it
            menus.findItem(R.id.todo_group).isVisible = it

            headerBinding.name =
                if (it) PreferencesHelper.fetchUserName(requireContext())
                else requireContext().getString(R.string.click_to_login)
        })

        handleUserProfile()
    }

    private fun handleUserProfile() {
        // TODO("由于 NavigationView menu.xml 不支持 dataBinding 绑定，目前未想到更好办法进行处理")
        mBinding.userProfileDrawer.setNavigationItemSelectedListener { menu ->
            when (menu.itemId) {
                R.id.favourite_article -> toFavourite(0)

                R.id.favourite_website -> toFavourite(1)

                R.id.todo_list -> launchTodoList()

                R.id.about -> showAboutUs()

                R.id.go_star -> starForUs()

                R.id.helper -> requireContext()
                    .alert(
                        String.format(
                            resources.getString(R.string.operate_helper),
                            ApplicationUtils.getAppVersionName(requireContext())
                        )
                    ) {
                        yesButton { dialog -> dialog.dismiss() }
                    }.show()

                R.id.login_out -> requireContext()
                    .alert("是否退出登录") {
                        yesButton {
                            mViewModel.loginout { requireContext().toast(it) }
                        }
                        noButton { }
                    }.show()
            }
            true
        }
    }

    private fun toFavourite(position: Int) {
        CollectionFragment.viewCollections(
            mNavController,
            R.id.action_mainFragment_to_collectionFragment,
            position
        )
        mBinding.drawer.closeDrawer(GravityCompat.START)
    }

    private fun launchTodoList() {
        mNavController.navigate(R.id.action_mainFragment_to_todoListFragment)
        mBinding.drawer.closeDrawer(GravityCompat.START)
    }

    private fun showAboutUs() {
        AboutUsDialog().apply {
            aboutUsHandler = { url ->
                WebsiteDetailFragment.viewDetail(
                    mNavController,
                    R.id.action_mainFragment_to_websiteDetailFragment,
                    url
                )
                mBinding.drawer.closeDrawer(GravityCompat.START)
            }
        }.show(childFragmentManager, "about")
    }

    private fun starForUs() {
        WebsiteDetailFragment.viewDetail(
            mNavController,
            R.id.action_mainFragment_to_websiteDetailFragment,
            "https://github.com/kukyxs/CoroutinesWanAndroid"
        )
        mBinding.drawer.closeDrawer(GravityCompat.START)
    }

    fun headerLogin(view: View) {
        if (mViewModel.hasLogin.value == false) {
            LoginDialogFragment().show(childFragmentManager, "login")
        }
    }

    fun openSettings(view: View) {
        mBinding.floatMenu.close(true)
        mBinding.drawer.openDrawer(GravityCompat.START)
    }

    fun showWxDialog(view: View) {
        WxDialog().show(childFragmentManager, "wx_code")
    }

    fun searchArticles(view: View) {
        mBinding.floatMenu.close(false)
        mNavController.navigate(R.id.action_mainFragment_to_searchFragment)
    }
}