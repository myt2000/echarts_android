package com.mcxinyu.echartsandroid.sample

import android.os.Bundle
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.gson.Gson
import com.mcxinyu.echartsandroid.sample.databinding.ActivityMainBinding
import com.mcxinyu.echartsandroid.webview.JavaScriptInterface
import com.mcxinyu.echartsandroid.webview.SampleMessage
import com.mcxinyu.echartsandroid.webview.addJavascriptInterface
import org.intellij.lang.annotations.Language

/**
 *
 * @author <a href=mailto:mcxinyu@foxmail.com>yuefeng</a> in 2022/2/24.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 隐藏标题栏
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        // 通过DataBinding技术绑定Activity的布局
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // 根据SwitchView的状态设置当前的主题模式（白天或黑夜）
        binding.switchView.setOnCheckedChangeListener { _, isChecked ->
            delegate.localNightMode =
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        }
        // 设置Echarts的背景颜色
        binding.echarts.setBackgroundColor(0)

        interactWithJs(binding)
        // 根据当前主题模式设置Echarts的主题。
        if (delegate.localNightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            binding.echarts.setThemeName("dark") { interactWithJs(binding) }
        } else {
            binding.echarts.registerThemeScript(wonderland) {
                binding.echarts.setThemeName("wonderland") { interactWithJs(binding) }
            }
        }
        // 根据SwitchViewLocal的状态选择加载本地的Echarts页面或者网络的Echarts页面，并显示相应的提示信息。
        binding.switchViewLocal.setOnCheckedChangeListener { buttonView, isChecked ->
            binding.echarts.setInitUrl(
                if (isChecked) "file:///android_asset/index.html"
                else "file:///android_asset/index_inner.html"
            )
            if (isChecked && buttonView.tag != "notified") {
                buttonView.tag = "notified"
                Toast.makeText(this@MainActivity, "首次网络加载 echarts.js，可能需要一些时间", Toast.LENGTH_SHORT).show()
            }
        }

        binding.m = option
    }

    private fun interactWithJs(binding: ActivityMainBinding) {
        // 该函数用于与Echarts的JavaScript交互。
        // 这个函数实现了一个JavaScriptInterface，将其绑定在Echarts中。
        // 在JavaScript中，可以通过这个interface调用Kotlin代码中的方法，从而实现JavaScript和Kotlin之间的通信。
        // 这个JavaScriptInterface中，使用了Gson库将从JavaScript传过来的数据转换为SampleMessage对象，然后判断SampleMessage的type属性是否为"showToast"。
        // 如果是的话，就在UI线程中显示一个Toast，内容为message.payload的值（如果不为空），否则显示默认值"just-call-on-message"。
        //最后，这个JavaScriptInterface返回null。
        binding.echarts.addJavascriptInterface(JavaScriptInterface("Messenger") {
            it?.let {
                val message = Gson().fromJson(it, SampleMessage::class.java)
                if (message.type == "showToast") {
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            message.payload?.toString() ?: "just-call-on-message",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            null
        })

        binding.echarts.runOnChecked {
            // 这段代码是一个 Kotlin lambda 表达式，在 Echarts 控件被初始化并完成渲染后，会自动执行。
            // 该 lambda 表达式的主要作用是在 Echarts 控件中注册一个点击事件的监听器，在用户点击某个系列时，将点击事件参数通过 JavaScriptInterface 发送给 Kotlin 代码中的方法进行处理。
            // 这段代码会通过 binding.echarts.runOnChecked 方法，等待 Echarts 控件完成初始化和渲染后再执行
            // binding.echarts.evaluateJavascript 方法会执行一段 JavaScript 脚本。这段脚本首先注释掉了一行代码，然后注册了一个点击事件的监听器。
            /**
             * 当用户点击某个系列时，会将点击事件参数通过 Messenger 调用 JavaScriptInterface 发送给 Kotlin 代码中的方法，发送的参数是一个包含 type 和 payload 两个属性的 JSON 字符串，
             * 其中 type 表示消息类型，payload 表示消息载荷。这里 type 的值为 "showToast"，表示需要在页面中显示一个 Toast，payload 的值是点击事件的数据对象（params.data），
             * 需要将其转换为 JSON 字符串后作为消息的载荷发送给 Kotlin 代码中的方法进行处理。
             * */
            /**
             * 这段 JavaScript 脚本会返回 void(0)，表示脚本执行完成。在这个 evaluateJavascript 方法的回调函数中，没有做任何处理，
             * 因此这个 lambda 表达式的作用是在 Echarts 控件中注册一个点击事件的监听器，并通过 JavaScriptInterface 将点击事件参数发送给 Kotlin 代码中的方法进行处理。
             * */
            binding.echarts.evaluateJavascript(
                """javascript:
                // Messenger.postMessage(${binding.echarts.echartsInstance}.getWidth());
                ${binding.echarts.echartsInstance}.on('click', 'series', (params) => {
                    Messenger.postMessage(JSON.stringify({
                      type: 'showToast',
                      payload: params.data,
                    }));
                });
                void(0);
            """.trimIndent()
            ) {}
        }
    }

    @Language("js")
    val option = """
        {
          title: {
            text: 'Accumulated Waterfall Chart'
          },
          legend: {
            data: ['Expenses', 'Income'],
            top: 24
          },
          grid: {
            left: '3%',
            right: '4%',
            bottom: '3%',
            containLabel: true
          },
          xAxis: {
            type: 'category',
            data: (function () {
              let list = [];
              for (let i = 1; i <= 11; i++) {
                list.push('Nov ' + i);
              }
              return list;
            })()
          },
          yAxis: {
            type: 'value'
          },
          series: [
            {
              name: 'Placeholder',
              type: 'bar',
              stack: 'Total',
              itemStyle: {
                borderColor: 'transparent',
                color: 'transparent'
              },
              emphasis: {
                itemStyle: {
                  borderColor: 'transparent',
                  color: 'transparent'
                }
              },
              data: [0, 900, 1245, 1530, 1376, 1376, 1511, 1689, 1856, 1495, 1292]
            },
            {
              name: 'Income',
              type: 'bar',
              stack: 'Total',
              label: {
                show: true,
                position: 'top'
              },
              data: [900, 345, 393, '-', '-', 135, 178, 286, '-', '-', '-']
            },
            {
              name: 'Expenses',
              type: 'bar',
              stack: 'Total',
              label: {
                show: true,
                position: 'bottom'
              },
              data: ['-', '-', '-', 108, 154, '-', '-', '-', 119, 361, 203]
            }
          ]
        }
    """.trimIndent()

    @Language("js")
    val wonderland = """
        (function (root, factory) {
            if (typeof define === 'function' && define.amd) {
                // AMD. Register as an anonymous module.
                define(['exports', 'echarts'], factory);
            } else if (typeof exports === 'object' && typeof exports.nodeName !== 'string') {
                // CommonJS
                factory(exports, require('echarts'));
            } else {
                // Browser globals
                factory({}, root.echarts);
            }
        }(this, function (exports, echarts) {
            var log = function (msg) {
                if (typeof console !== 'undefined') {
                    console && console.error && console.error(msg);
                }
            };
            if (!echarts) {
                log('ECharts is not Loaded');
                return;
            }
            echarts.registerTheme('wonderland', {
                "color": [
                    "#4ea397",
                    "#22c3aa",
                    "#7bd9a5",
                    "#d0648a",
                    "#f58db2",
                    "#f2b3c9"
                ],
                "backgroundColor": "rgba(255,255,255,0)",
                "textStyle": {},
                "title": {
                    "textStyle": {
                        "color": "#666666"
                    },
                    "subtextStyle": {
                        "color": "#999999"
                    }
                },
                "line": {
                    "itemStyle": {
                        "borderWidth": "2"
                    },
                    "lineStyle": {
                        "width": "3"
                    },
                    "symbolSize": "8",
                    "symbol": "emptyCircle",
                    "smooth": false
                },
                "radar": {
                    "itemStyle": {
                        "borderWidth": "2"
                    },
                    "lineStyle": {
                        "width": "3"
                    },
                    "symbolSize": "8",
                    "symbol": "emptyCircle",
                    "smooth": false
                },
                "bar": {
                    "itemStyle": {
                        "barBorderWidth": 0,
                        "barBorderColor": "#ccc"
                    }
                },
                "pie": {
                    "itemStyle": {
                        "borderWidth": 0,
                        "borderColor": "#ccc"
                    }
                },
                "scatter": {
                    "itemStyle": {
                        "borderWidth": 0,
                        "borderColor": "#ccc"
                    }
                },
                "boxplot": {
                    "itemStyle": {
                        "borderWidth": 0,
                        "borderColor": "#ccc"
                    }
                },
                "parallel": {
                    "itemStyle": {
                        "borderWidth": 0,
                        "borderColor": "#ccc"
                    }
                },
                "sankey": {
                    "itemStyle": {
                        "borderWidth": 0,
                        "borderColor": "#ccc"
                    }
                },
                "funnel": {
                    "itemStyle": {
                        "borderWidth": 0,
                        "borderColor": "#ccc"
                    }
                },
                "gauge": {
                    "itemStyle": {
                        "borderWidth": 0,
                        "borderColor": "#ccc"
                    }
                },
                "candlestick": {
                    "itemStyle": {
                        "color": "#d0648a",
                        "color0": "transparent",
                        "borderColor": "#d0648a",
                        "borderColor0": "#22c3aa",
                        "borderWidth": "1"
                    }
                },
                "graph": {
                    "itemStyle": {
                        "borderWidth": 0,
                        "borderColor": "#ccc"
                    },
                    "lineStyle": {
                        "width": "1",
                        "color": "#cccccc"
                    },
                    "symbolSize": "8",
                    "symbol": "emptyCircle",
                    "smooth": false,
                    "color": [
                        "#4ea397",
                        "#22c3aa",
                        "#7bd9a5",
                        "#d0648a",
                        "#f58db2",
                        "#f2b3c9"
                    ],
                    "label": {
                        "color": "#ffffff"
                    }
                },
                "map": {
                    "itemStyle": {
                        "areaColor": "#eeeeee",
                        "borderColor": "#999999",
                        "borderWidth": 0.5
                    },
                    "label": {
                        "color": "#28544e"
                    },
                    "emphasis": {
                        "itemStyle": {
                            "areaColor": "rgba(34,195,170,0.25)",
                            "borderColor": "#22c3aa",
                            "borderWidth": 1
                        },
                        "label": {
                            "color": "#349e8e"
                        }
                    }
                },
                "geo": {
                    "itemStyle": {
                        "areaColor": "#eeeeee",
                        "borderColor": "#999999",
                        "borderWidth": 0.5
                    },
                    "label": {
                        "color": "#28544e"
                    },
                    "emphasis": {
                        "itemStyle": {
                            "areaColor": "rgba(34,195,170,0.25)",
                            "borderColor": "#22c3aa",
                            "borderWidth": 1
                        },
                        "label": {
                            "color": "#349e8e"
                        }
                    }
                },
                "categoryAxis": {
                    "axisLine": {
                        "show": true,
                        "lineStyle": {
                            "color": "#cccccc"
                        }
                    },
                    "axisTick": {
                        "show": false,
                        "lineStyle": {
                            "color": "#333"
                        }
                    },
                    "axisLabel": {
                        "show": true,
                        "color": "#999999"
                    },
                    "splitLine": {
                        "show": true,
                        "lineStyle": {
                            "color": [
                                "#eeeeee"
                            ]
                        }
                    },
                    "splitArea": {
                        "show": false,
                        "areaStyle": {
                            "color": [
                                "rgba(250,250,250,0.05)",
                                "rgba(200,200,200,0.02)"
                            ]
                        }
                    }
                },
                "valueAxis": {
                    "axisLine": {
                        "show": true,
                        "lineStyle": {
                            "color": "#cccccc"
                        }
                    },
                    "axisTick": {
                        "show": false,
                        "lineStyle": {
                            "color": "#333"
                        }
                    },
                    "axisLabel": {
                        "show": true,
                        "color": "#999999"
                    },
                    "splitLine": {
                        "show": true,
                        "lineStyle": {
                            "color": [
                                "#eeeeee"
                            ]
                        }
                    },
                    "splitArea": {
                        "show": false,
                        "areaStyle": {
                            "color": [
                                "rgba(250,250,250,0.05)",
                                "rgba(200,200,200,0.02)"
                            ]
                        }
                    }
                },
                "logAxis": {
                    "axisLine": {
                        "show": true,
                        "lineStyle": {
                            "color": "#cccccc"
                        }
                    },
                    "axisTick": {
                        "show": false,
                        "lineStyle": {
                            "color": "#333"
                        }
                    },
                    "axisLabel": {
                        "show": true,
                        "color": "#999999"
                    },
                    "splitLine": {
                        "show": true,
                        "lineStyle": {
                            "color": [
                                "#eeeeee"
                            ]
                        }
                    },
                    "splitArea": {
                        "show": false,
                        "areaStyle": {
                            "color": [
                                "rgba(250,250,250,0.05)",
                                "rgba(200,200,200,0.02)"
                            ]
                        }
                    }
                },
                "timeAxis": {
                    "axisLine": {
                        "show": true,
                        "lineStyle": {
                            "color": "#cccccc"
                        }
                    },
                    "axisTick": {
                        "show": false,
                        "lineStyle": {
                            "color": "#333"
                        }
                    },
                    "axisLabel": {
                        "show": true,
                        "color": "#999999"
                    },
                    "splitLine": {
                        "show": true,
                        "lineStyle": {
                            "color": [
                                "#eeeeee"
                            ]
                        }
                    },
                    "splitArea": {
                        "show": false,
                        "areaStyle": {
                            "color": [
                                "rgba(250,250,250,0.05)",
                                "rgba(200,200,200,0.02)"
                            ]
                        }
                    }
                },
                "toolbox": {
                    "iconStyle": {
                        "borderColor": "#999999"
                    },
                    "emphasis": {
                        "iconStyle": {
                            "borderColor": "#666666"
                        }
                    }
                },
                "legend": {
                    "textStyle": {
                        "color": "#999999"
                    }
                },
                "tooltip": {
                    "axisPointer": {
                        "lineStyle": {
                            "color": "#cccccc",
                            "width": 1
                        },
                        "crossStyle": {
                            "color": "#cccccc",
                            "width": 1
                        }
                    }
                },
                "timeline": {
                    "lineStyle": {
                        "color": "#4ea397",
                        "width": 1
                    },
                    "itemStyle": {
                        "color": "#4ea397",
                        "borderWidth": 1
                    },
                    "controlStyle": {
                        "color": "#4ea397",
                        "borderColor": "#4ea397",
                        "borderWidth": 0.5
                    },
                    "checkpointStyle": {
                        "color": "#4ea397",
                        "borderColor": "#3cebd2"
                    },
                    "label": {
                        "color": "#4ea397"
                    },
                    "emphasis": {
                        "itemStyle": {
                            "color": "#4ea397"
                        },
                        "controlStyle": {
                            "color": "#4ea397",
                            "borderColor": "#4ea397",
                            "borderWidth": 0.5
                        },
                        "label": {
                            "color": "#4ea397"
                        }
                    }
                },
                "visualMap": {
                    "color": [
                        "#d0648a",
                        "#22c3aa",
                        "#adfff1"
                    ]
                },
                "dataZoom": {
                    "backgroundColor": "rgba(255,255,255,0)",
                    "dataBackgroundColor": "rgba(222,222,222,1)",
                    "fillerColor": "rgba(114,230,212,0.25)",
                    "handleColor": "#cccccc",
                    "handleSize": "100%",
                    "textStyle": {
                        "color": "#999999"
                    }
                },
                "markPoint": {
                    "label": {
                        "color": "#ffffff"
                    },
                    "emphasis": {
                        "label": {
                            "color": "#ffffff"
                        }
                    }
                }
            });
        }));
    """.trimIndent()
}